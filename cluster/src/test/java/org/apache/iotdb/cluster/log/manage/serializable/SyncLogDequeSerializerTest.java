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
package org.apache.iotdb.cluster.log.manage.serializable;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.iotdb.cluster.common.IoTDBTest;
import org.apache.iotdb.cluster.common.TestLogApplier;
import org.apache.iotdb.cluster.common.TestUtils;
import org.apache.iotdb.cluster.log.Log;
import org.apache.iotdb.cluster.log.LogApplier;
import org.apache.iotdb.cluster.log.Snapshot;
import org.apache.iotdb.cluster.log.manage.MemoryLogManager;
import org.junit.Test;

public class SyncLogDequeSerializerTest extends IoTDBTest {

  private Set<Log> appliedLogs = new HashSet<>();
  private LogApplier logApplier = new TestLogApplier() {
    @Override
    public void apply(Log log) {
      appliedLogs.add(log);
    }
  };

  private MemoryLogManager buildMemoryLogManager() {
    return new MemoryLogManager(logApplier) {
      @Override
      public Snapshot getSnapshot() {
        return null;
      }

      @Override
      public void takeSnapshot() {

      }
    };
  }

  @Test
  public void test() {
    byte[] b = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11};
    ByteBuffer byteBuffer = ByteBuffer.wrap(b);
    byteBuffer.getLong();
    System.out.println(byteBuffer.getInt());
  }

  @Test
  public void testReadAndWrite() {
    SyncLogDequeSerializer syncLogDequeSerializer = new SyncLogDequeSerializer();
    MemoryLogManager memoryLogManager = buildMemoryLogManager();
    List<Log> testLogs1 = TestUtils.prepareNodeLogs(10);

    for (Log log : testLogs1) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(10, syncLogDequeSerializer.getLogSizeDeque().size());

    List<Log> testLogs2 = TestUtils.prepareNodeLogs(5);

    for (Log log : testLogs2) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(15, syncLogDequeSerializer.getLogSizeDeque().size());

    syncLogDequeSerializer.close();
  }

  @Test
  public void testRecovery() {
    SyncLogDequeSerializer syncLogDequeSerializer = new SyncLogDequeSerializer();
    MemoryLogManager memoryLogManager = buildMemoryLogManager();
    int logNum = 10;
    List<Log> testLogs1 = TestUtils.prepareNodeLogs(logNum);

    for (Log log : testLogs1) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(logNum, syncLogDequeSerializer.getLogSizeDeque().size());

    syncLogDequeSerializer.close();

    // recovery
    syncLogDequeSerializer = new SyncLogDequeSerializer();
    LogManagerMeta managerMeta = syncLogDequeSerializer.recoverMeta();
    assertEquals(memoryLogManager.getMeta(), managerMeta);

    List<Log> logDeque = syncLogDequeSerializer.recoverLog();
    assertEquals(logNum, logDeque.size());

    for (int i = 0; i < logNum; i++) {
      assertEquals(testLogs1.get(i), logDeque.get(i));
    }
  }

  @Test
  public void testRecoveryAfterRemoveFirst() {
    SyncLogDequeSerializer syncLogDequeSerializer = new SyncLogDequeSerializer();
    MemoryLogManager memoryLogManager = buildMemoryLogManager();
    List<Log> testLogs1 = TestUtils.prepareNodeLogs(10);

    for (Log log : testLogs1) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(10, syncLogDequeSerializer.getLogSizeDeque().size());


    List<Log> testLogs2 = TestUtils.prepareNodeLogs(5);

    for (Log log : testLogs2) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(15, syncLogDequeSerializer.getLogSizeDeque().size());

    syncLogDequeSerializer.removeFirst(3);

    syncLogDequeSerializer.close();

    // recovery
    List<Log> logs = syncLogDequeSerializer.recoverLog();
    assertEquals(12, logs.size());

    for (int i = 0; i < 7; i++) {
      assertEquals(testLogs1.get(i + 3), logs.get(i));
    }

    for (int i = 0; i < 5; i++) {
      assertEquals(testLogs2.get(i), logs.get(i + 7));
    }
  }

  @Test
  public void testDeleteLogs() {
    SyncLogDequeSerializer syncLogDequeSerializer = new SyncLogDequeSerializer();
    MemoryLogManager memoryLogManager = buildMemoryLogManager();
    syncLogDequeSerializer.setMaxRemovedLogSize(10);
    List<Log> testLogs1 = TestUtils.prepareNodeLogs(10);

    for (Log log : testLogs1) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(10, syncLogDequeSerializer.getLogSizeDeque().size());

    List<Log> testLogs2 = TestUtils.prepareNodeLogs(5);

    for (Log log : testLogs2) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    syncLogDequeSerializer.removeFirst(3);

    assertEquals(12, syncLogDequeSerializer.getLogSizeDeque().size());

    syncLogDequeSerializer.close();
  }

  @Test
  public void testDeleteLogsByRecovery() {
    SyncLogDequeSerializer syncLogDequeSerializer = new SyncLogDequeSerializer();
    syncLogDequeSerializer.setMaxRemovedLogSize(10);

    MemoryLogManager memoryLogManager = buildMemoryLogManager();
    List<Log> testLogs1 = TestUtils.prepareNodeLogs(10);

    for (Log log : testLogs1) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(10, syncLogDequeSerializer.getLogSizeDeque().size());


    List<Log> testLogs2 = TestUtils.prepareNodeLogs(5);

    for (Log log : testLogs2) {
      memoryLogManager.appendLog(log);
      syncLogDequeSerializer.addLast(log, memoryLogManager.getMeta());
    }

    assertEquals(15, syncLogDequeSerializer.getLogSizeDeque().size());

    syncLogDequeSerializer.removeFirst(3);

    syncLogDequeSerializer.close();

    // recovery
    syncLogDequeSerializer = new SyncLogDequeSerializer();
    List<Log> logs = syncLogDequeSerializer.recoverLog();
    assertEquals(12, logs.size());

    for (int i = 0; i < 7; i++) {
      assertEquals(testLogs1.get(i + 3), logs.get(i));
    }

    for (int i = 0; i < 5; i++) {
      assertEquals(testLogs2.get(i), logs.get(i + 7));
    }
  }
}