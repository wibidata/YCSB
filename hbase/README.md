This assumes HBase is already installed.

### Create a table

Open the hbase shell

    hbase shell

Once inside the shell, create a table called 'usertable' with
a column family called 'family' and exit shell.

    create 'usertable', 'family'
    exit

### Configure HBase

Copy your hbase-site.xml to hbase/src/main/conf/ or modify the
existing one and add your hbase settings.

### Run YCSB workloads

bin/ycsb load hbase -P workloads/workloada -p columnfamily=family -s > output

bin/ycsb run hbase -P workloads/workloada -p columnfamily=family -s > output

