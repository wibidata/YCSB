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

import java.util.Properties;

import org.apache.avro.specific.SpecificRecord;

public class SampleAvroValueController implements AvroValueController{
  @Override
  public void init(Properties p) { }

  @Override
  public SpecificRecord generateRecord(String dbKey, String fieldKey) {
    // This implementation just creates a record that has a member
    // holding the db key and field name. This makes it easy to check
    // in isValidSpecificRecord() below.
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
        && sampleRecord.getDbkey().equals(dbKey)
        && sampleRecord.getField().equals(fieldKey);
  }
}
