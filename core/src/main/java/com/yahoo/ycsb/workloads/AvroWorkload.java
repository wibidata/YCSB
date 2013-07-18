package com.yahoo.ycsb.workloads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBWrapper;
import com.yahoo.ycsb.WorkloadException;
import com.yahoo.ycsb.avro.AvroDBClient;
import com.yahoo.ycsb.avro.AvroValueController;
import com.yahoo.ycsb.measurements.Measurements;
import org.apache.avro.specific.SpecificRecord;

/**
 * The core benchmark scenario, modified to use AVRO types. Represents a set of clients doing
 * simple CRUD operations, like the CoreWorkload, but using AVRO data. See {@link CoreWorkload}.
 *
 * Properties are the same as the CoreWorkload with the following exceptions:
 *
 * <UL>
 * <LI><b>fieldlength</b>: this field is unused.
 * <LI><b>avrocontrollerclass</b>: this should be a fully qualified class name that implements
 *     {@link com.yahoo.ycsb.avro.AvroValueController}.
 * </ul>
 *
 * <p>Must be used with a DB client that implements {@link AvroDBClient}.</p>
 *
 * @author WibiData, Inc.
 */
public class AvroWorkload extends CoreWorkload {
  protected AvroValueController mAvroController;

  /**
   * Property to specify the avro controller class to use for generating and checking Avro records.
   */
  public static final String AVRO_CONTROLLER_PROPERTY = "avrocontrollerclass";

  public void init(Properties p) throws WorkloadException
  {
    super.init(p);
    try {
      Class controllerClass = Class.forName(p.getProperty(AVRO_CONTROLLER_PROPERTY));
      mAvroController = (AvroValueController)controllerClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new WorkloadException(
          "AvroWorkloads must specify a fully qualified AvroValueController class in "
          + AVRO_CONTROLLER_PROPERTY + " properter",
          e);
    } catch (IllegalAccessException e) {
      throw new WorkloadException(
          "AvroWorkloads must specify a fully qualified AvroValueController class in "
              + AVRO_CONTROLLER_PROPERTY + " properter",
          e);
    } catch (InstantiationException e) {
      throw new WorkloadException(
          "AvroWorkloads must specify a fully qualified AvroValueController class in "
              + AVRO_CONTROLLER_PROPERTY + " properter",
          e);
    }
  }

  /**
   * Helper function to build an array of values.
   *
   * @param dbKey The dbKey to generate Records for.
   * @return A map of field keys to SpecificRecords to store under those fields.
   */
  HashMap<String, SpecificRecord> buildAvroValues(String dbKey) {
    HashMap<String, SpecificRecord> values=new HashMap<String, SpecificRecord>();

    for (int i=0; i<fieldcount; i++)
    {
      String fieldkey="field"+i;
      SpecificRecord data = mAvroController.generateRecord(dbKey, fieldkey);
      values.put(fieldkey,data);
    }
    return values;
  }

  /**
   * Helper function to build an update to a random field.
   *
   * @param dbKey The dbKey to generate the Record for.
   * @return A map of a field key to a SpecificRecords to store under that field.
   */
  HashMap<String, SpecificRecord> buildAvroUpdate(String dbKey) {
    //update a random field
    HashMap<String, SpecificRecord> value = new HashMap<String, SpecificRecord>();
    String fieldname = "field"+fieldchooser.nextString();
    SpecificRecord data = mAvroController.generateRecord(dbKey, fieldname);
    value.put(fieldname,data);
    return value;
  }

  public boolean doInsert(DB db, Object threadstate)
  {
    final AvroDBClient adb = (AvroDBClient)db;
    int keynum=keysequence.nextInt();
    String dbkey = buildKeyName(keynum);
    HashMap<String, SpecificRecord> values = buildAvroValues(dbkey);
    if (adb.insertAvro(table, dbkey, values) == 0) {
      return true;
    } else {
      return false;
    }
  }

  public void doTransactionInsert(DB db)
  {
    final AvroDBClient adb = (AvroDBClient)db;
    //choose the next key
    int keynum=transactioninsertkeysequence.nextInt();

    String dbkey = buildKeyName(keynum);

    HashMap<String, SpecificRecord> values = buildAvroValues(dbkey);

    adb.insertAvro(table,dbkey,values);
  }

  public void doTransactionRead(DB db)
  {
    final AvroDBClient adb = (AvroDBClient)db;
    //choose a random key
    int keynum = nextKeynum();

    String keyname = buildKeyName(keynum);

    HashSet<String> fields=null;

    if (!readallfields)
    {
      //read a random field
      String fieldname="field"+fieldchooser.nextString();

      fields=new HashSet<String>();
      fields.add(fieldname);
    }

    adb.readAvro(table,keyname,fields,new HashMap<String,SpecificRecord>());
  }

  public void doTransactionReadModifyWrite(DB db)
  {
    final AvroDBClient adb = (AvroDBClient)db;
    //choose a random key
    int keynum = nextKeynum();

    String keyname = buildKeyName(keynum);

    HashSet<String> fields=null;

    if (!readallfields)
    {
      //read a random field
      String fieldname="field"+fieldchooser.nextString();

      fields=new HashSet<String>();
      fields.add(fieldname);
    }

    HashMap<String,SpecificRecord> values;

    if (writeallfields)
    {
      //new data for all the fields
      values = buildAvroValues(keyname);
    }
    else
    {
      //update a random field
      values = buildAvroUpdate(keyname);
    }

    //do the transaction

    long st=System.nanoTime();

    adb.readAvro(table,keyname,fields,new HashMap<String,SpecificRecord>());

    adb.updateAvro(table,keyname,values);

    long en=System.nanoTime();

    Measurements.getMeasurements().measure("READ-MODIFY-WRITE", (int)((en-st)/1000));
  }

  public void doTransactionScan(DB db)
  {
    throw new UnsupportedOperationException("Scan not yet implemented.");
  }

  public void doTransactionUpdate(DB db)
  {
    final AvroDBClient adb = (AvroDBClient)db;
    //choose a random key
    int keynum = nextKeynum();

    String keyname=buildKeyName(keynum);

    HashMap<String, SpecificRecord> values;

    if (writeallfields)
    {
      //new data for all the fields
      values = buildAvroValues(keyname);
    }
    else
    {
      //update a random field
      values = buildAvroUpdate(keyname);
    }

    adb.updateAvro(table,keyname,values);
  }
}
