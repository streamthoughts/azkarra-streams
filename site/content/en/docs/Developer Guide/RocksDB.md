---
date: 2021-03-03
title: "RocksDB"
linkTitle: "RocksDB"
weight: 100
description: >
  How to tune internal RocksDB state stores ?
---

Internally, KafkaStreams relies on an embedded key-value store so-called [RocksDB](https://rocksdb.org/) to provided persistent storage.
Depending on the throughput of your application, you may want to tune internal RocksDB instances.

Kafka Streams allows you to customize the RocksDB settings for a given Store by implementing the interface `org.apache.kafka.streams.state.RocksDBConfigSetter`.

The custom implementation must then be configured using : 

```java
streamsConfig.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, CustomRocksDBConfig.class)
```

**Azkarra Streams** provides a built-in `io.streamthoughts.azkarra.commons.rocksdb.AzkarraRocksDBConfigSetter` which is automatically configured if no one is already provided.

`AzkarraRocksDBConfigSetter` allows overriding not only some default RocksDB options but also to 
enable log statistics for performance debugging and shared memory usage.

Available properties are : 

| Property                               | Type    | Default | Description                                             |
|----------------------------------------|---------|---------|---------------------------------------------------------|
| `rocksdb.stats.enable`                 | boolean |         | Enable RocksDB statistics                               |
| `rocksdb.stats.dump.period.sec`        | integer |         | The RocksDB statistics dump period in seconds.          |
| `rocksdb.log.dir`                      | string  |         | The RocksDB log directory                               |
| `rocksdb.log.level`                    | string  |         | The RocksDB log level (see org.rocksdb.InfoLogLevel).   |
| `rocksdb.log.max.file.size`            | integer |         | The RocksDB maximum log file size.                      |
| `rocksdb.max.write.buffer.number`      | integer |         | The maximum number of memtables build up in memory, before they flush to SST files.|
| `rocksdb.write.buffer.size`            | long    |         | The size of a single memtable. |
| `rocksdb.memory.managed`               | boolean | false   | Enable automatic memory management across all RocksDB instances. |
| `rocksdb.memory.write.buffer.ratio`    | double  | 0.5     | The ratio of total cache memory which will be reserved for write buffer manager. This property is only used when 'rocksdb.memory.managed' is set to true. |
| `rocksdb.memory.high.prio.pool.ratio`  | double  | 0.1     | The ratio of cache memory that is reserved for high priority blocks (e.g.: indexes, filters and compressions blocks). |
| `rocksdb.memory.strict.capacity.limit` | boolean | false   | Create a block cache with strict capacity limit, i.e., insert to the cache will fail when cache is full. This property is only used when 'rocksdb.memory.managed' is set to true or 'rocksdb.block.cache.size' is set.|
| `rocksdb.block.cache.size`             | long    | false   | The total size to be used for caching uncompressed data blocks.|
| `rocksdb.compaction.style`             | string  |         | The compaction style.|
| `rocksdb.compression.type`             | string  |         | The compression type.|
| `rocksdb.files.open`                   | long    |         | The maximum number of open files that can be used per RocksDB instance.|
| `rocksdb.background.thread.flush.pool` | integer    |      | The number of threads to be used for the background flush process.|
| `rocksdb.background.compaction.flush.pool` | integer|      | The number of threads to be used for the background compaction process.|
| `rocksdb.max.background.flushes`       | integer |         | The maximum number of concurrent flush operations.|     

Note that all properties described above are optional.

RocksDB properties can be passed either using default configuration :

```
azrkarra {
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

or programmatically through the method `StreamsExecutionEnvironment#addConfiguration`:

```java

LocalStreamExecutionEnvironment()
    .create()
    .addConfiguration(new RocksDBConfig()
          .withMemoryManaged(true)
          .withMemoryWriteBufferRatio(0.3)
          .withBlockCacheSize(500 * 1024 * 1024)
          .withStatsEnable()
          .withLogDir("/tmp/rocksdb-logs")  
    );
```

Please read the official documentation for more information: [RocksDB Tuning Guide](https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide)