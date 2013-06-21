## Benchmarking Kiji Schema using YCSB

KijiSchema simplifies real-time storage and retrieval of diverse data from
primitive types to objects, time-series and event streams. KijiSchema handles
challenges with serialization, schema design & evolution, and meta data
management common in NoSQL storage solutions. It is based on top of HBase.
For more information, please see www.kiji.org

### 1. Setup On a Kiji Cluster

Install Hadoop, Hbase and Kiji on your cluster.
For instructions, see http://www.kiji.org/getstarted/#Downloads

It is recommended that you increase the max number of zookeeper client
connections to accomodate the parralelism of YCSB.

### 2. Set Up YCSB and create a Kiji table

Clone the YCSB git repository and compile:

    export YCSB_HOME=/path/to/your/YCSB/dir
    git clone git@github.com:renuka-apte/YCSB.git $YCSB_HOME
    cd $YCSB_HOME
    mvn clean package
    kiji create-table --layout=kiji/resources/layout.json --num-regions=<#servers in cluster> \
    --table=kiji://.env/default/kijitable

Note: In order to ensure that the appropriate hbase-site.xml file in your environment gets picked
up, you might have to move the `$YCSB_HOME/hbase/src/main/conf/hbase-site.xml` file out of the way.
You can add a `conf` directory under `$YCSB_HOME/kiji` with the settings for connecting to your
cluster.

### 3. Run YCSB

Now you are ready to run! First, load the data:

    bin/ycsb load kiji -P workloads/readmostly_smallupdates -p kiji.columnFamily="vals" -s > result_load.dat

Then, run the workload:

    bin/ycsb run kiji -P workloads/readmostly_smallupdates -p kiji.columnFamily="vals" -s > result_run.dat

### Optional: Loading in parallel

In order to partition the input workload, YCSB lets you run multiple clients in parallel. This allows you to
increase the load throughput. You can refer to the instructions
[here](https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload-in-Parallel) for this. Specifically,
you will need to set the two properties: insertstart, insertcount per client as shown in the example.

### Understanding the Results

Look [here](https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload#step-6-execute-the-workload)
to understand the results stored in `result_load.dat` and `result_run.dat`.

In addition, during the cleanup call, we measure the time taken by the final flush, and store it
as [FLUSH] in the measurements.

### Configuring the Workload

Look [here](https://github.com/brianfrankcooper/YCSB/wiki/Core-Properties) for instructions to configure
the workload. There are additional properties you can use in the CoreWorkload file
[here](https://github.com/brianfrankcooper/YCSB/blob/master/core/src/main/java/com/yahoo/ycsb/workloads/CoreWorkload.java).

### Custom Schemas

To facilitate custom Avro schemas, we've created a number of new classes:

  * `AvroWorkload` - similar to the CoreWorkload but with Avro data.
  * `AvroDBClient` - An interface for DB clients that support Avro operations. Presently only Kiji.
  * `AvroValueController` - An interface for factories to generate Avro records.

To use this, one must take the following steps:

  1. Select an Avro schema to test.
  2. Make a table layout for it and install it. For example:

      <pre>CREATE TABLE kijitable WITH DESCRIPTION 'A table for YCSB with SampleRecord avro values'
      ROW KEY FORMAT HASH PREFIXED(2)
      WITH LOCALITY GROUP default
        WITH DESCRIPTION 'Main locality group' (
        MAXVERSIONS = 1,
        TTL = FOREVER,
        INMEMORY = false,
        COMPRESSED WITH NONE,
        MAP TYPE FAMILY vals CLASS com.yahoo.ycsb.avro.SampleRecord WITH DESCRIPTION 'YCSB values'
      );</pre>
   Note the `com.yahoo.ycsb.avro.SampleRecord` in this example. That refers to a generated Avro
   record included as an example with this distribution.
  3. Create a workload file to use. This should be like the existing workload files written for
  CoreWorkload, but should define the property `avrocontrollerclass`, which should be the fully
  qualified class name of an implementation of `AvroValueController` on the runtime classpath, and
  define `workload=com.yahoo.ycsb.workloads.AvroWorkload`. Consult
  `YCSB/workloads/avro_readmostly_smallupdates` for an example. This uses the
  `com.yahoo.ycsb.avro.SampleAvroValueController` class included in this codebase. Here's an
  excerpt:

      <pre>
      # --AVRO SPECIFIC SECTION--
      # Note that this workload is AvroWorkload instead of CoreWorkload.
      workload=com.yahoo.ycsb.workloads.AvroWorkload
      # This points to a simple controller which outputs an Avro schema with two string fields
      avrocontrollerclass=com.yahoo.ycsb.avro.SampleAvroValueController
      </pre>

  4. Use the workload to load the table. E.g.:

        bin/ycsb load kiji -P workloads/avro_readmostly_smallupdates -p kiji.columnFamily="vals" -s > result_load.dat

  5. Use the workload to run the experiment.

  6. To compare different schemas, use two different tables each with a map family of the
  appropriate schema. Write two different workload files, each pointing to a different
  `AvroValueController` (or add an `AvroValueController` class that can generate different kinds of
  schemas based on some property).
