/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.cluster.log;

import java.nio.ByteBuffer;
import java.util.Objects;
import org.apache.iotdb.cluster.log.logtypes.LogType;

/**
 * Log records operations that are made on this cluster. Each log records 4 longs: currLogIndex
 * (may be replaced by previousLogIndex + 1), currLogTerm, previousLogIndex, previousLogTerm, so
 * that the logs in a cluster will form a log chain and abnormal operations can thus be
 * distinguished and removed.
 */
public abstract class Log {

  protected static final int DEFAULT_BUFFER_SIZE = 4096;
  private long currLogIndex;
  private long currLogTerm;
  private long previousLogIndex;
  private long previousLogTerm;

  protected LogType logType;

  public abstract ByteBuffer serialize();

  public abstract void deserialize(ByteBuffer buffer);

  public enum Types {
    // TODO-Cluster#348 support more logs
    // DO CHECK LogParser when you add a new type of log
    ADD_NODE, PHYSICAL_PLAN, CLOSE_FILE, REMOVE_NODE
  }

  public long getPreviousLogIndex() {
    return previousLogIndex;
  }

  public void setPreviousLogIndex(long previousLogIndex) {
    this.previousLogIndex = previousLogIndex;
  }

  public long getPreviousLogTerm() {
    return previousLogTerm;
  }

  public void setPreviousLogTerm(long previousLogTerm) {
    this.previousLogTerm = previousLogTerm;
  }

  public long getCurrLogIndex() {
    return currLogIndex;
  }

  public void setCurrLogIndex(long currLogIndex) {
    this.currLogIndex = currLogIndex;
  }

  public long getCurrLogTerm() {
    return currLogTerm;
  }

  public void setCurrLogTerm(long currLogTerm) {
    this.currLogTerm = currLogTerm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Log log = (Log) o;
    return currLogIndex == log.currLogIndex &&
        currLogTerm == log.currLogTerm &&
        previousLogIndex == log.previousLogIndex &&
        previousLogTerm == log.previousLogTerm;
  }

  @Override
  public int hashCode() {
    return Objects.hash(currLogIndex, currLogTerm, previousLogIndex, previousLogTerm);
  }

  public LogType getLogType(){
    return logType;
  }
}
