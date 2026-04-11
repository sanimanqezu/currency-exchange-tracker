#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Start Up Batch script
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ] ; then
  JAVACMD=java
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
DOWNLOAD_URL=

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
  DOWNLOAD_URL=$(grep -E "^wrapperUrl=" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/wrapperUrl=//')
fi

if [ -r "$MAVEN_WRAPPER_JAR" ]; then
  :
elif [ -n "$DOWNLOAD_URL" ]; then
  echo "Downloading Maven Wrapper from $DOWNLOAD_URL"
  if command -v curl > /dev/null; then
    curl -fsSL "$DOWNLOAD_URL" -o "$MAVEN_WRAPPER_JAR"
  elif command -v wget > /dev/null; then
    wget -q "$DOWNLOAD_URL" -O "$MAVEN_WRAPPER_JAR"
  else
    echo "Error: Neither curl nor wget is available. Cannot download Maven Wrapper."
    exit 1
  fi
fi

DIST_DIR="$HOME/.m2/wrapper"
DIST_URL=$(grep -E "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | sed 's/distributionUrl=//')
DIST_FILENAME=$(basename "$DIST_URL")
DIST_NAME="${DIST_FILENAME%-bin.zip}"
DIST_PATH="$DIST_DIR/$DIST_NAME"

if [ ! -d "$DIST_PATH" ]; then
  mkdir -p "$DIST_DIR"
  echo "Downloading Maven from $DIST_URL"
  if command -v curl > /dev/null; then
    curl -fsSL "$DIST_URL" -o "$DIST_DIR/$DIST_FILENAME"
  elif command -v wget > /dev/null; then
    wget -q "$DIST_URL" -O "$DIST_DIR/$DIST_FILENAME"
  fi
  unzip -q "$DIST_DIR/$DIST_FILENAME" -d "$DIST_DIR"
fi

MAVEN_HOME="$DIST_PATH"
exec "$MAVEN_HOME/bin/mvn" "$@"
