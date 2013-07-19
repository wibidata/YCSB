/**
 * Copyright (c) 2013 WibiData Inc. All rights reserved.
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

package com.yahoo.ycsb.avro;

import java.util.HashMap;
import java.util.Set;

import org.apache.avro.specific.SpecificRecord;

/**
 * An interface for a database backend which supports AVRO operations as opposed to ByteIterator
 * operations. Should be used by classes that extend com.yahoo.ycsb.DB.
 *
 * @author WibiData, Inc.
 */
public interface AvroDBClient {
  /**
   * Read a record from the database. Each field/avro-value pair from the result will be stored in a HashMap.
   *
   * @param table The name of the table
   * @param key The record key of the record to read.
   * @param fields The list of fields to read, or null for all of them
   * @param result A HashMap of field/avro-value pairs for the result
   * @return Zero on success, a non-zero error code on error or "not found".
   */
  public int readAvro(String table, String key, Set<String> fields, HashMap<String,SpecificRecord> result);

  /**
   * Update a record in the database. Any field/avro-value pairs in the specified values HashMap will be written into the record with the specified
   * record key, overwriting any existing values with the same field name.
   *
   * @param table The name of the table
   * @param key The record key of the record to write.
   * @param values A HashMap of field/value pairs to update in the record
   * @return Zero on success, a non-zero error code on error.  See this class's description for a discussion of error codes.
   */
  public int updateAvro(String table, String key, HashMap<String,SpecificRecord> values);

  /**
   * Insert a record in the database. Any field/avro-value pairs in the specified values HashMap will be written into the record with the specified
   * record key.
   *
   * @param table The name of the table
   * @param key The record key of the record to insert.
   * @param values A HashMap of field/value pairs to insert in the record
   * @return Zero on success, a non-zero error code on error.  See this class's description for a discussion of error codes.
   */
  public int insertAvro(String table, String key, HashMap<String,SpecificRecord> values);
}
