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

echo "JAVA_VERSION:"
java -version

# If the user does not provide a custom configuration,
# copy the default configuration bundle with the distribution.
AZKARRA_CONF=/etc/azkarra/azkarra.conf
if [ ! -f "$AZKARRA_CONF" ]; then
    mkdir -p /etc/azkarra/ && cp "$AZKARRA_HOME"/etc/azkarra.conf $AZKARRA_CONF;
fi

export STREAMS_JVM_OPTS="$STREAMS_JVM_OPTS -Dconfig.file=$AZKARRA_CONF"

# Build azkarra agrs from environment variables.
AZKARRA_ARGS=$@
for var in $(printenv | sort | grep AZKARRA_); do
  echo $var
  name=${var%"="*}
  confValue=${var#*"="}
  confName=$(echo ${name#*CONFIG_} | awk '{print tolower($0)}')
  confName=$(echo "$confName" | sed -r 's/_/./g')
  AZKARRA_ARGS="$AZKARRA_ARGS --$confName $confValue"
done

if [ -z "$STREAMS_HEAP_OPTS" ]; then
  # Configure JVM to size HEAP based on the container memory.
  export STREAMS_HEAP_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=50 -XX:MaxRAMPercentage=90"
fi

if [ -z "$LOG_DIR" ]; then
  export LOG_DIR="/var/log/azkarra"
fi

# Exec
exec "$AZKARRA_HOME/bin/azkarra-streams-start.sh" $AZKARRA_ARGS