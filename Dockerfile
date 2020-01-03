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
#
# Azkarra Streams Worker
# VERSION 0.5.0
FROM azul/zulu-openjdk:13

ARG azkarraVersion
ARG azkarraBranch
ARG azkarraCommit

ENV AZKARRA_HOME="/usr/local/azkarra" \
    AZKARRA_VERSION="${azkarraVersion}" \
    AZKARRA_COMMIT="${azkarraCommit}" \
    AZKARRA_BRANCH="${azkarraBranch}"

RUN mkdir -p ${AZKARRA_HOME} \
    mkdir -p ${AZKARRA_HOME}/share/java/azkarra-worker

# Copy dependencies
COPY ./docker-build/dependencies/ ${AZKARRA_HOME}/share/java/azkarra-worker

# Copy resources
COPY ./docker-entrypoint.sh /
COPY ./docker-build/resources/ ${AZKARRA_HOME}/

# Copy classes
COPY ./docker-build/classes/ ${AZKARRA_HOME}/share/java/azkarra-worker

LABEL io.streamthoughts.docker.name="azkarra-streams" \
      io.streamthoughts.docker.version=$AZKARRA_VERSION \
      io.streamthoughts.docker.branch=$AZKARRA_BRANCH \
      io.streamthoughts.docker.commit=$AZKARRA_COMMIT

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]