/**
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

package org.apache.iotdb.db.engine.merge;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.iotdb.db.conf.IoTDBDescriptor;

public class MergeManager {

  private static final MergeManager INSTANCE = new MergeManager();

  private AtomicInteger threadNum = new AtomicInteger();
  private ThreadPoolExecutor pool;

  private MergeManager() {
    pool =
        (ThreadPoolExecutor) Executors.newFixedThreadPool(
            IoTDBDescriptor.getInstance().getConfig().getMergeConcurrentThreads(),
            r -> new Thread(r, "mergeThread-" + threadNum.getAndIncrement()));
  }

  public static MergeManager getINSTANCE() {
    return INSTANCE;
  }

  public void submit(MergeTask mergeTask) {
    pool.submit(mergeTask);
  }
}