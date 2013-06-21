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

/**
 * This interface is a factory that generates and checks Avro values for {@link AvroDBClient}s.
 *
 * <p>Users who want to test the performance of a backend with Avro should create an implementation
 * of this interface to generate the sorts of Avro records they want.</p>
 *
 * <p>Presently, this is tied to Specific rather than Generic records.</p>
 *
 * <p>The class must implement a default, empty constructor to be initialized correctly.</p>
 *
 * @author WibiData, Inc.
 */
public interface AvroValueController {
  /**
   * Initializes the state of the Controller, based on the properties.
   *
   * <p>Will be called once, immediately after object creation, with the same set of properties
   * as the workload.</p>
   *
   * @param p The Properties of this YCSB execution, including workload properties and commandline
   *     properties.
   */
  public void init(Properties p);

  /**
   * Given a db and field key (e.g. a column name), generates a SpecificRecord to store in it.
   *
   * @param dbKey The dbKey that will be used for storing the SpecificRecord.
   * @param fieldKey The field key under the dbKey where the SpecificRecord will be stored.
   * @return A SpecificRecord object to be stored.
   */
  SpecificRecord generateRecord(String dbKey, String fieldKey);

  /**
   * Checks a retrieved SpecificRecord for validity.
   *
   * @param record The SpecificRecord to check.
   * @param dbKey The dbKey under which the record is stored.
   * @param fieldKey The fieldKey under the dbKey where the record is stored.
   * @return true if the record is valid. False otherwise.
   */
  boolean isValidSpecificRecord(SpecificRecord record, String dbKey, String fieldKey);
}
