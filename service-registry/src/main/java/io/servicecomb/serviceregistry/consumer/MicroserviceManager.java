/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.serviceregistry.consumer;

import java.util.Map;

import com.google.common.eventbus.Subscribe;

import io.servicecomb.foundation.common.concurrent.ConcurrentHashMapEx;
import io.servicecomb.serviceregistry.task.event.PeriodicPullEvent;
import io.servicecomb.serviceregistry.task.event.RecoveryEvent;

public class MicroserviceManager {
  private AppManager appManager;

  private String appId;

  // key: microserviceName
  private Map<String, MicroserviceVersions> versionsByName = new ConcurrentHashMapEx<>();

  public MicroserviceManager(AppManager appManager, String appId) {
    this.appManager = appManager;
    this.appId = appId;

    appManager.getEventBus().register(this);
  }

  public MicroserviceVersions getOrCreateMicroserviceVersions(String microserviceName) {
    return versionsByName.computeIfAbsent(microserviceName, name -> {
      MicroserviceVersions instance = new MicroserviceVersions(appManager, appId, microserviceName);
      instance.submitPull();
      return instance;
    });
  }

  public MicroserviceVersionRule getOrCreateMicroserviceVersionRule(String microserviceName,
      String versionRule) {
    MicroserviceVersions microserviceVersions = getOrCreateMicroserviceVersions(microserviceName);

    return microserviceVersions.getOrCreateMicroserviceVersionRule(versionRule);
  }

  @Subscribe
  public void periodicPull(PeriodicPullEvent event) {
    refreshInstances();
  }

  @Subscribe
  public void serviceRegistryRecovery(RecoveryEvent event) {
    refreshInstances();
  }

  protected void refreshInstances() {
    for (MicroserviceVersions versions : versionsByName.values()) {
      versions.submitPull();
    }
  }
}
