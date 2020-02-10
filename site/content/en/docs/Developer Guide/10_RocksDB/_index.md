---
date: 2020-02-12
title: "RocksDB"
linkTitle: "RocksDB"
weight: 10
description: >
  How to tune internal RocksDB state stores ?
---

Internally, KafkaStreams relies on an embedded key-value store so-called [RocksDB](https://rocksdb.org/) to provided persistent storage.
Depending of the throughput of your application, you may want to tune internal RocksDB instances.

Kafka Streams allows you to customize the RocksDB settings for a given Store by implementing the interface `org.apache.kafka.streams.state.RocksDBConfigSetter`.

The custom implementation must then be configured using : 

```java
streamsConfig.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class)
```

**Azkarra Streams** provides a built-in `DefaultRocksDBConfigSetter` which is automatically configured if no one is already provided.

`DefaultRocksDBConfigSetter` allows you to override not only some default RocksDB options but also to enable log statistics for performance debugging.

Available properties are : 

| Property                            | Type    | Description                                     |
|-------------------------------------|---------|-------------------------------------------------|
|  `rocksdb.stats.enable`             | boolean | Enable RocksDB statistics                               |
|  `rocksdb.stats.dump.period.sec`    | integer | The RocksDB statistics dump period in seconds.          |
|  `rocksdb.log.dir`                  | string  | The RocksDB log directory                               |
|  `rocksdb.log.level`                | string  | The RocksDB log level (see org.rocksdb.InfoLogLevel).   |
|  `rocksdb.log.max.file.size`        | integer | The RocksDB maximum log file size.                      |
|  `rocksdb.max.write.buffer.number`  | integer | The maximum number of memtables build up in memory, before they flush to SST files.          |
|  `rocksdb.write.buffer.size`        | long    | The size of a single memtable.                          |

Note that all properties described above are optional.


RocksDB properties can be passed either using default configuration :

```
context {
  streams {
    rocksdb {
      stats.enable = false
      stats.dumpPeriodSec = 30
      log {
        dir = "/var/log/kafka-streams/rocksdb"
        file.size = 104857600
      }
    }
  }
}
```

or programmatically through the method `StreamsExecutionEnvironment#setRocksDBConfig`:

```java
context.defaultExecutionEnvironment()
    .setRocksDBSettings(
        RocksDBConfig.withStatsEnable()
                     .withLogDir("/tmp/rocksdb-logs")
    );
```

Please read official documentation for more information: [RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide)