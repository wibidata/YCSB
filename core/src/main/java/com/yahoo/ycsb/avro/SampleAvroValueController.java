package com.yahoo.ycsb.avro;

import java.util.Properties;

import org.apache.avro.specific.SpecificRecord;

public class SampleAvroValueController implements AvroValueController{
  @Override
  public void init(Properties p) { }

  @Override
  public SpecificRecord generateRecord(String dbKey, String fieldKey) {
    SampleRecord record = new SampleRecord();
    record.setDbkey(dbKey);
    // We don't permit nulls in the record.
    if (null == fieldKey) {
      fieldKey = "";
    }
    record.setField(fieldKey);
    return record;
  }

  @Override
  public boolean isValidSpecificRecord(SpecificRecord record, String dbKey, String fieldKey) {
    SampleRecord sampleRecord = (SampleRecord)record;
    return (null != sampleRecord)
        && (sampleRecord.getDbkey() == dbKey)
        && sampleRecord.getField() == fieldKey;
  }
}
