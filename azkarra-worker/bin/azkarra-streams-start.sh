#!/bin/sh
# Copyright 2019 StreamThoughts.
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

usage="USAGE: $0 [-daemon] [--property value]*"

DIST_DIR=$(dirname "$(readlink -f $0)")/..

while [ $# -gt 0 ]; do
  COMMAND=$1
  case $COMMAND in
    -daemon)
      DAEMON_MODE="true"
      shift
      ;;
    -help)
      echo $usage
      exit 1
      ;;
    *)
      break
      ;;
  esac
done

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# JMX settings
if [ -z "$STREAMS_JMX_OPTS" ]; then
  STREAMS_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false "
fi

# JMX port to use
if [ -n "$JMX_PORT" ]; then
  STREAMS_JMX_OPTS="$STREAMS_JMX_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT "
fi

# Log directory to use
if [ -z "$LOG_DIR" ]; then
    LOG_DIR="$DIST_DIR/logs"
fi

# Create logs directory
if [ ! -d "$LOG_DIR" ]; then
    mkdir -p "$LOG_DIR"
fi

# Log4j2 settings
if [ -z "$LOG4J2_OPTS" ]; then
    LOG4J2_OPTS="-Dlog4j.configurationFile=$DIST_DIR/etc/log4j2.xml"
fi

LOG4J2_OPTS="-Dlog4j.logsDir=$LOG_DIR $LOG4J2_OPTS"

if [ -n "$CLASSPATH" ]; then
  CLASSPATH="$CLASSPATH:$DIST_DIR/share/java/azkarra-worker/*"
else
  CLASSPATH="$DIST_DIR/share/java/azkarra-worker/*"
fi

# JVM GC options
if [ -z "$STREAMS_GC_LOG_OPTS" ]; then
    if [ -z "$GC_LOG_FILE_NAME" ]; then
        GC_LOG_FILE_NAME="`date +%F_%H-%M-%S`-gc.log"
    fi
    JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed -E -n 's/.* version "([0-9]*).*$/\1/p')
    if [ "$JAVA_MAJOR_VERSION" -ge "9" ] ; then
      STREAMS_GC_LOG_OPTS="-Xlog:gc*:file=$LOG_DIR/$GC_LOG_FILE_NAME:time,tags:filecount=10,filesize=102400"
    else
      STREAMS_GC_LOG_OPTS="-Xloggc:$LOG_DIR/$GC_LOG_FILE_NAME -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M"
    fi
fi
# JVM Debug options
if [ -z "$STREAMS_JVM_HEAP_DUMP_OPTS" ]; then
    STREAMS_JVM_HEAP_DUMP_OPTS="-XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$LOG_DIR"
fi

# JVM performance options
if [ -z "$STREAMS_JVM_PERFORMANCE_OPTS" ]; then
  STREAMS_JVM_PERFORMANCE_OPTS="-server -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -Djava.awt.headless=true"
fi

# Generic jvm settings you want to add
if [ -z "$STREAMS_JVM_OPTS" ]; then
  STREAMS_JVM_OPTS=""
fi

# Memory options
if [ -z "$STREAMS_HEAP_OPTS" ]; then
  STREAMS_HEAP_OPTS="-Xmx256M"
fi

STREAMS_JVM_OPTS="$STREAMS_JVM_OPTS $STREAMS_GC_LOG_OPTS $STREAMS_JVM_HEAP_DUMP_OPTS $STREAMS_HEAP_OPTS $STREAMS_JVM_PERFORMANCE_OPTS $STREAMS_JMX_OPTS"

MAIN_CLASS="io.streamthoughts.azkarra.AzkarraWorker"
# Launch mode
if [ "x$DAEMON_MODE" = "xtrue" ]; then
  nohup $JAVACMD $STREAMS_JVM_OPTS $LOG4J2_OPTS -cp "$CLASSPATH" $MAIN_CLASS "$@" > console.out 2>&1 < /dev/null &
else
  exec $JAVACMD $STREAMS_JVM_OPTS $LOG4J2_OPTS -cp "$CLASSPATH" $MAIN_CLASS "$@"
fi