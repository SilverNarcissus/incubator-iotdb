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

package org.apache.iotdb.db.writelog.recover;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.iotdb.db.engine.filenode.TsFileResource;
import org.apache.iotdb.db.engine.memtable.IMemTable;
import org.apache.iotdb.db.engine.modification.Deletion;
import org.apache.iotdb.db.engine.modification.ModificationFile;
import org.apache.iotdb.db.engine.version.VersionController;
import org.apache.iotdb.db.exception.ProcessorException;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.db.qp.physical.crud.DeletePlan;
import org.apache.iotdb.db.qp.physical.crud.InsertPlan;
import org.apache.iotdb.db.writelog.io.ILogReader;
import org.apache.iotdb.db.writelog.manager.MultiFileLogNodeManager;
import org.apache.iotdb.db.writelog.node.WriteLogNode;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.write.record.TSRecord;
import org.apache.iotdb.tsfile.write.record.datapoint.DataPoint;
import org.apache.iotdb.tsfile.write.schema.FileSchema;

public class LogReplayer {

  private String processorName;
  private String insertFilePath;
  private ModificationFile modFile;
  private VersionController versionController;
  private TsFileResource currentTsFileResource;
  private FileSchema fileSchema;
  private IMemTable recoverMemTable;

  public LogReplayer(String processorName, String insertFilePath,
      ModificationFile modFile,
      VersionController versionController,
      TsFileResource currentTsFileResource,
      FileSchema fileSchema, IMemTable memTable) {
    this.processorName = processorName;
    this.insertFilePath = insertFilePath;
    this.modFile = modFile;
    this.versionController = versionController;
    this.currentTsFileResource = currentTsFileResource;
    this.fileSchema = fileSchema;
    this.recoverMemTable = memTable;
  }

  public void replayLogs() throws ProcessorException {
    WriteLogNode logNode;
    try {
      logNode = MultiFileLogNodeManager.getInstance().getNode(
          processorName + new File(insertFilePath).getName());
    } catch (IOException e) {
      throw new ProcessorException(e);
    }
    ILogReader logReader = logNode.getLogReader();
    try {
      while (logReader.hasNext()) {
        PhysicalPlan plan = logReader.next();
        if (plan instanceof InsertPlan) {
          replayInsert((InsertPlan) plan, recoverMemTable);
        } else if (plan instanceof DeletePlan) {
          replayDelete((DeletePlan) plan, recoverMemTable);
        }
      }
    } catch (IOException e) {
      throw new ProcessorException("Cannot replay logs", e);
    }
  }

  private void replayDelete(DeletePlan deletePlan, IMemTable recoverMemTable) throws IOException {
    List<Path> paths = deletePlan.getPaths();
    for (Path path : paths) {
      recoverMemTable.delete(path.getDevice(), path.getMeasurement(), deletePlan.getDeleteTime());
      modFile.write(new Deletion(path.getFullPath(),
          versionController.nextVersion(),deletePlan.getDeleteTime()));
    }
  }

  private void replayInsert(InsertPlan insertPlan, IMemTable recoverMemTable) {
    TSRecord tsRecord = new TSRecord(insertPlan.getTime(), insertPlan.getDeviceId());
    if (currentTsFileResource != null) {
      // the last chunk group may contain the same data with the logs, ignore such logs
      if (currentTsFileResource.getEndTime(insertPlan.getDeviceId()) >= insertPlan.getTime()) {
        return;
      }
      currentTsFileResource.updateTime(insertPlan.getDeviceId(), insertPlan.getTime());
    }
    String[] measurementList = insertPlan.getMeasurements();
    String[] insertValues = insertPlan.getValues();

    for (int i = 0; i < measurementList.length; i++) {
      TSDataType dataType = fileSchema.getMeasurementDataType(measurementList[i]);
      String value = insertValues[i];
      DataPoint dataPoint = DataPoint.getDataPoint(dataType, measurementList[i], value);
      tsRecord.addTuple(dataPoint);
    }
    recoverMemTable.insert(tsRecord);
  }
}