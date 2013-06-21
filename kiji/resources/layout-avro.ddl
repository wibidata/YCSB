CREATE TABLE kijitable WITH DESCRIPTION 'A table for YCSB with SampleRecord avro values'
ROW KEY FORMAT HASH PREFIXED(2)
WITH LOCALITY GROUP default
  WITH DESCRIPTION 'Main locality group' (
  MAXVERSIONS = 1,
  TTL = FOREVER,
  INMEMORY = false,
  COMPRESSED WITH NONE,
  MAP TYPE FAMILY vals CLASS com.yahoo.ycsb.avro.SampleRecord WITH DESCRIPTION 'YCSB values'
);
