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
package org.apache.iotdb.db.engine.upgrade;

import org.apache.iotdb.db.concurrent.WrappedRunnable;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.service.UpgradeSevice;
import org.apache.iotdb.db.utils.UpgradeUtils;
import org.apache.iotdb.tsfile.fileSystem.FSFactoryProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeTask extends WrappedRunnable {

  private final TsFileResource upgradeResource;
  private static final Logger logger = LoggerFactory.getLogger(UpgradeTask.class);
  private static final String COMMA_SEPERATOR = ",";


  public UpgradeTask(TsFileResource upgradeResource) {
    this.upgradeResource = upgradeResource;
  }

  @Override
  public void runMayThrow() {
    try {
      upgradeResource.readLock();
      String tsfilePathBefore = upgradeResource.getFile().getAbsolutePath();
      String tsfilePathAfter = UpgradeUtils.getUpgradeFileName(upgradeResource.getFile());

      UpgradeLog.writeUpgradeLogFile(
          tsfilePathBefore + COMMA_SEPERATOR + UpgradeCheckStatus.BEGIN_UPGRADE_FILE);
      upgradeResource.writeLock();
      try {
        FSFactoryProducer.getFSFactory().getFile(tsfilePathBefore).delete();
        FSFactoryProducer.getFSFactory()
            .moveFile(FSFactoryProducer.getFSFactory().getFile(tsfilePathAfter),
                FSFactoryProducer.getFSFactory().getFile(tsfilePathBefore));
        UpgradeLog.writeUpgradeLogFile(
            tsfilePathBefore + COMMA_SEPERATOR + UpgradeCheckStatus.UPGRADE_SUCCESS);
        FSFactoryProducer.getFSFactory().getFile(tsfilePathAfter).getParentFile().delete();
      } finally {
        upgradeResource.writeUnlock();
      }
      UpgradeSevice.setCntUpgradeFileNum(UpgradeSevice.getCntUpgradeFileNum() - 1);
      logger.info("Upgrade completes, file path:{} , the remaining upgraded file num: {}",
          tsfilePathBefore, UpgradeSevice.getCntUpgradeFileNum());
    } catch (Exception e) {
      logger.error("meet error when upgrade file:{}", upgradeResource.getFile().getAbsolutePath(),
          e);
    }
  }
}
