/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.yahoo.ycsb.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import com.google.common.base.Preconditions;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;
import com.yahoo.ycsb.avro.AvroDBClient;
import com.yahoo.ycsb.measurements.Measurements;
import org.apache.avro.specific.SpecificRecord;
import org.apache.hadoop.hbase.HConstants;

import org.kiji.schema.EntityId;
import org.kiji.schema.KConstants;
import org.kiji.schema.Kiji;
import org.kiji.schema.KijiBufferedWriter;
import org.kiji.schema.KijiDataRequest;
import org.kiji.schema.KijiDataRequestBuilder;
import org.kiji.schema.KijiRowData;
import org.kiji.schema.KijiRowScanner;
import org.kiji.schema.KijiTable;
import org.kiji.schema.KijiTableReader;
import org.kiji.schema.KijiURI;
import org.kiji.schema.hbase.HBaseScanOptions;

/**
 * Kiji Schema Client for YCSB.
 * Kiji Schema is a database schema layer on top of HBase.
 * For more information, please see http://www.kiji.org
 */
public class KijiClient extends com.yahoo.ycsb.DB implements AvroDBClient {
  private Kiji kijiInstance;
  private KijiTable kijiTable;
  private KijiBufferedWriter writer;
  private KijiTableReader reader;
  private Properties properties;
  private String columnFamily;

  public static final int DB_ERROR = -1;
  public static final int NOT_FOUND = -2;
  public static final int OK = 0;


  /**
   * Set up connections to the Kiji table in order to read and write.
   *
   * @throws DBException if anything blows up in Kiji land.
   */
  public void init() throws DBException {
    properties = getProperties();
    String kijiUriString = properties.getProperty("kiji.instance",
        KConstants.DEFAULT_INSTANCE_NAME);
    try {
      kijiInstance = Kiji.Factory.open(
          KijiURI.newBuilder().withInstanceName(kijiUriString).build());
    } catch (IOException e) {
      e.printStackTrace();
      throw new DBException();
    }

    String tableName = Preconditions.checkNotNull(
        properties.getProperty("table"),
        "Please provide the table parameter using -p");
    columnFamily = Preconditions.checkNotNull(
        properties.getProperty("kiji.columnFamily"),
        "Please provide the kiji.columnFamily parameter using -p");

    try {
      kijiTable = kijiInstance.openTable(tableName);
    } catch (IOException e) {
      e.printStackTrace();
      throw new DBException();
    }

    try {
      writer = kijiTable.getWriterFactory().openBufferedWriter();
      writer.setBufferSize(1024*1024*12);
    } catch (IOException e) {
      e.printStackTrace();
      throw new DBException();
    }
    reader = kijiTable.openTableReader();
  }

  /**
   * Read a single record.
   *
   * @param table The name of the table
   * @param key The record key of the record to read.
   * @param fields The list of fields to read, or null for all of them
   * @param result A HashMap of field/value pairs for the result
   * @return 0 on success, DB_ERROR or NOT_FOUND otherwise
   */
  public int read(String table, String key, Set<String> fields,
                  HashMap<String,ByteIterator> result) {
    if (!table.equals(kijiTable.getName())) {
      throw new RuntimeException(String.format("Table supplied by read does" +
          " not match user parameter\ntable supplied = %s, required=%s",
          table, kijiTable.getName()));
    }

    final EntityId eid = kijiTable.getEntityId(key);

    final KijiDataRequestBuilder reqBuilder = KijiDataRequest.builder();
    KijiDataRequestBuilder.ColumnsDef colDef = reqBuilder.newColumnsDef()
        .withMaxVersions(1);
    if (fields == null) {
      colDef.addFamily(columnFamily);
    } else {
      for (String qualifiers: fields) {
        colDef = colDef.add(columnFamily, qualifiers);
      }
    }
    final KijiDataRequest dataRequest = reqBuilder.build();

    try {
      final KijiRowData rowData = reader.get(eid, dataRequest);
      if (rowData == null) {
        return NOT_FOUND;
      }
      if (fields != null) {
        for (String field: fields) {
          if (!rowData.containsColumn(columnFamily, field)) {
            return NOT_FOUND;
          }
        }
      }
      Set<String> qualifiers = rowData.getQualifiers(columnFamily);
      for (String qualifier: qualifiers) {
        result.put(qualifier,
            new StringByteIterator(rowData.getMostRecentValue(columnFamily,
                qualifier).toString()));
      }
    } catch (IOException ioe) {
      return DB_ERROR;
    }
    return OK;
  }

  /**
   * Scan a contiguous set of records.
   *
   * @param table The name of the table
   * @param startkey The record key of the first record to read.
   * @param recordcount The number of records to read
   * @param fields The list of fields to read, or null for all of them
   * @param result A Vector of HashMaps, where each HashMap is a set field/value
   *               pairs for one record
   * @return 0 on success, DB_ERROR otherwise.
   */
  public int scan(String table, String startkey, int recordcount,
      Set<String> fields, Vector<HashMap<String,ByteIterator>> result) {
    final KijiDataRequestBuilder reqBuilder = KijiDataRequest.builder();
    KijiDataRequestBuilder.ColumnsDef colDef = reqBuilder.newColumnsDef();
    if (fields == null) {
          colDef.addFamily(columnFamily);
    } else {
      for (String qualifiers: fields) {
        colDef = colDef.add(columnFamily, qualifiers);
      }
    }
    final KijiDataRequest dataRequest = reqBuilder.build();
    final EntityId eid = kijiTable.getEntityId(startkey);
    KijiTableReader.KijiScannerOptions options =
        new KijiTableReader.KijiScannerOptions().setStartRow(eid);
    HBaseScanOptions hBaseScanOptions = new HBaseScanOptions();
    hBaseScanOptions.setClientBufferSize(recordcount);

    final KijiRowScanner rowScanner;
    try {
      rowScanner = reader.getScanner(dataRequest,
          options.setHBaseScanOptions(hBaseScanOptions));
    } catch (IOException e) {
      e.printStackTrace();
      return DB_ERROR;
    }
    int numresults = 0;
    Set<String> rowFields = fields;
    for (KijiRowData rowData : rowScanner) {
      if (fields == null) {
        rowFields = rowData.getQualifiers(columnFamily);
      }
      HashMap<String,ByteIterator> fieldMap =
          new HashMap<String, ByteIterator>();
      for (String qualifier: rowFields) {
        String fieldRes;
        try {
          fieldRes = rowData.getMostRecentValue(columnFamily, qualifier)
              .toString();
        } catch (IOException e) {
          return DB_ERROR;
        }
        fieldMap.put(qualifier, new StringByteIterator(fieldRes));
      }
      result.add(fieldMap);
      numresults++;
      if (numresults == recordcount) {
        break;
      }
    }
    try {
      rowScanner.close();
    } catch (IOException e) {
      e.printStackTrace();
      return DB_ERROR;
    }
    return OK;
  }

  /**
   * Update a single existing entry in the table with new data.
   *
   * @param table The name of the table
   * @param key The record key of the record to write.
   * @param values A HashMap of field/value pairs to update in the record
   * @return 0 on success, DB_ERROR otherwise.
   */
  public int update(String table, String key,
                    HashMap<String,ByteIterator> values) {
    return insert(table, key, values);
  }

  public int insert(String table,
                    String key, HashMap<String, ByteIterator> data) {
    Preconditions.checkArgument(!data.isEmpty());

    if (!table.equals(kijiTable.getName())) {
      throw new RuntimeException(String.format("Table supplied by read does" +
          " not match user parameter\ntable supplied = %s, required = %s",
          table, kijiTable.getName()));
    }

    final EntityId eid = kijiTable.getEntityId(key);

    for (String qualifier: data.keySet()) {
      try {
        writer.put(eid, columnFamily, qualifier, data.get(qualifier)
            .toString());
      } catch (IOException e) {
        e.printStackTrace();
        return DB_ERROR;
      }
    }
    return OK;
  }

  /**
   * Delete a single record entirely.
   *
   * @param table The name of the table
   * @param key The record key of the record to delete.
   * @return 0 on success, DB_ERROR otherwise.
   */
  public int delete(String table, String key) {
    if (!table.equals(kijiTable.getName())) {
      throw new RuntimeException(String.format("Table supplied by read does" +
          " not match user parameter\ntable supplied = %s, required=%s",
          table, kijiTable.getName()));
    }
    try{
      writer.deleteRow(kijiTable.getEntityId(key),
          HConstants.LATEST_TIMESTAMP);
    } catch (IOException ioe) {
      return DB_ERROR;
    }
    return OK;
  }

  /**
   * Flush any outstanding writes and close the table reader and writer so
   * that the htable underneath will be released.
   *
   * @throws DBException if anything blows up in kiji land.
   */
  public void cleanup() throws DBException
  {
    Measurements _measurements = Measurements.getMeasurements();
    try {
      long st=System.nanoTime();
      writer.flush();
      long en=System.nanoTime();
      _measurements.measure("FLUSH", (int)((en-st)/1000));
    } catch (IOException e) {
      throw new DBException(e);
    }
    try {
      reader.close();
      writer.close();
    } catch (IOException e) {
      throw new DBException(e);
    }
  }

  @Override
  public int readAvro(String table, String key, Set<String> fields, HashMap<String, SpecificRecord> result) {
    if (!table.equals(kijiTable.getName())) {
      throw new RuntimeException(String.format("Table supplied by read does" +
          " not match user parameter\ntable supplied = %s, required=%s",
          table, kijiTable.getName()));
    }

    final EntityId eid = kijiTable.getEntityId(key);

    final KijiDataRequestBuilder reqBuilder = KijiDataRequest.builder();
    KijiDataRequestBuilder.ColumnsDef colDef = reqBuilder.newColumnsDef()
        .withMaxVersions(1);
    if (fields == null) {
      colDef.addFamily(columnFamily);
    } else {
      for (String qualifiers: fields) {
        colDef = colDef.add(columnFamily, qualifiers);
      }
    }
    final KijiDataRequest dataRequest = reqBuilder.build();

    try {
      final KijiRowData rowData = reader.get(eid, dataRequest);
      if (rowData == null) {
        return NOT_FOUND;
      }
      if (fields != null) {
        for (String field: fields) {
          if (!rowData.containsColumn(columnFamily, field)) {
            return NOT_FOUND;
          }
        }
      }
      Set<String> qualifiers = rowData.getQualifiers(columnFamily);
      for (String qualifier: qualifiers) {
        // We just read in the data. Let the workload check the result.
        result.put(qualifier,
            rowData.<SpecificRecord>getMostRecentValue(columnFamily, qualifier));
      }
    } catch (IOException ioe) {
      return DB_ERROR;
    }
    return OK;
  }

  @Override
  public int updateAvro(String table, String key, HashMap<String, SpecificRecord> values) {
    return insertAvro(table, key, values);
  }

  @Override
  public int insertAvro(String table, String key, HashMap<String, SpecificRecord> values) {
    Preconditions.checkArgument(!values.isEmpty());

    if (!table.equals(kijiTable.getName())) {
      throw new RuntimeException(String.format("Table supplied by read does" +
          " not match user parameter\ntable supplied = %s, required = %s",
          table, kijiTable.getName()));
    }

    final EntityId eid = kijiTable.getEntityId(key);

    for (String qualifier : values.keySet()) {
      try {
        writer.put(eid, columnFamily, qualifier, values.get(qualifier));
      } catch (IOException e) {
        e.printStackTrace();
        return DB_ERROR;
      }
    }
    return OK;
  }
}
