## Benchmarking Kiji Schema using YCSB

KijiSchema simplifies real-time storage and retrieval of diverse data from
primitive types to objects, time-series and event streams. KijiSchema handles
challenges with serialization, schema design & evolution, and meta data
management common in NoSQL storage solutions. It is based on top of HBase.
For more information, please see www.kiji.org

### 1. Setup On a Kiji Cluster

Install Hadoop, Hbase and Kiji on your cluster.
For instructions, see http://www.kiji.org/getstarted/#Downloads

### 2. Set Up YCSB and create a Kiji table

Clone the YCSB git repository and compile:

    export YCSB_HOME=/path/to/your/YCSB/dir
    git clone git@github.com:renuka-apte/YCSB.git $YCSB_HOME
    cd $YCSB_HOME
    mvn clean package
    kiji create-table --layout=kiji/resources/layout.json --num-regions=<#servers in cluster> \
    --table=kiji://.env/default/kijitable

Note: In order to ensure that the appropriate hbase-site.xml file in your environment gets picked
up, you might have to move the $YCSB_HOME/hbase/src/main/conf/hbase-site.xml file out of the way.

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
