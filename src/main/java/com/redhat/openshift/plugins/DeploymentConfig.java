package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.COMMA;
import static com.redhat.openshift.plugins.Constants.EMPTY_STRING;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_IMAGE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_PORTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_MOUNT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_PVC_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_TYPE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_VARS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.SPACE;

import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class DeploymentConfig {
  private JSONObject deploymentConfigObject;
  private TaskContext taskContext;
  private Map<String, JSONArray> volumeMountListMap = new HashMap<>();

  DeploymentConfig(TaskContext taskContext) {
    JSONObject deploymentConfigObjectTempl =
        new JSONObject(Templates.getInstance(taskContext).getDeploymentConfigTemplate());
    deploymentConfigObjectTempl
        .getJSONObject(Constants.METADATA)
        .getJSONObject(Constants.LABELS)
        .put(Constants.APP, taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.METADATA)
        .put(
            Constants.NAME, taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.METADATA)
        .put(Constants.NAMESPACE, taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.SELECTOR)
        .put(
            Constants.DEPLOYMENTCONFIG,
            taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.SELECTOR)
        .put(
            Constants.DEPLOYMENTCONFIG,
            taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.TEMPLATE)
        .getJSONObject(Constants.METADATA)
        .getJSONObject(Constants.LABELS)
        .put(Constants.APP, taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    deploymentConfigObjectTempl
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.TEMPLATE)
        .getJSONObject(Constants.METADATA)
        .getJSONObject(Constants.LABELS)
        .put(
            Constants.DEPLOYMENTCONFIG,
            taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME));
    this.deploymentConfigObject = deploymentConfigObjectTempl;
    this.taskContext = taskContext;
  }

  private void configureStorageVolumes() {
    JSONArray storageList = new JSONArray();
    JSONArray volumeMountList;
    String storageMountContainerName;
    String storageType;
    JSONObject containerStorageJson = new JSONObject();
    JSONObject volumeMountJson;
    String elementNumber;

    for (String key : taskContext.getConfigurationMap().keySet()) {
      if (key.startsWith(OPENSHIFT_CONTAINER_STORAGE_NAME)) {
        elementNumber = StringUtils.remove(key, OPENSHIFT_CONTAINER_STORAGE_NAME);
        storageMountContainerName =
            taskContext
                .getConfigurationMap()
                .get(OPENSHIFT_CONTAINER_STORAGE_MOUNT + elementNumber);
        storageType =
            taskContext.getConfigurationMap().get(OPENSHIFT_CONTAINER_STORAGE_TYPE + elementNumber);
        if (Objects.equals(storageType, Constants.EMPTYDIR)) {
          containerStorageJson =
              new JSONObject(Templates.getInstance(taskContext).getEmptyDirTemplate());
          containerStorageJson.put(Constants.NAME, taskContext.getConfigurationMap().get(key));
        } else if (Objects.equals(storageType, Constants.PVC)) {
          containerStorageJson =
              new JSONObject(Templates.getInstance(taskContext).getPersistentVolumeClaimTemplate());
          containerStorageJson
              .put(Constants.NAME, taskContext.getConfigurationMap().get(key))
              .put(
                  Constants.PERSISTENT_VOLUME_CLAIM,
                  new JSONObject(
                      "{\"claimName\":"
                          + taskContext
                              .getConfigurationMap()
                              .get(OPENSHIFT_CONTAINER_STORAGE_PVC_NAME + elementNumber)
                          + "}"));
        }
        volumeMountJson =
            new JSONObject(Templates.getInstance(taskContext).getVolumeMountTemplate());
        volumeMountJson
            .put(Constants.NAME, taskContext.getConfigurationMap().get(key))
            .put(
                Constants.MOUNT_PATH,
                taskContext
                    .getConfigurationMap()
                    .get(OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT + elementNumber));
        storageList.put(containerStorageJson);
        volumeMountList = volumeMountListMap.get(storageMountContainerName);
        if (volumeMountList == null) {
          volumeMountList = new JSONArray();
        }
        volumeMountList.put(volumeMountJson);
        volumeMountListMap.put(storageMountContainerName, volumeMountList);
      }
    }
    deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.TEMPLATE)
        .getJSONObject(Constants.SPEC)
        .put(Constants.VOLUMES, storageList);
  }

  void configureContainers() {
    String elementNumber;
    JSONArray containers = new JSONArray();

    configureStorageVolumes();
    for (String key : taskContext.getConfigurationMap().keySet()) {
      if (key.startsWith(OPENSHIFT_CONTAINER_IMAGE)) {
        elementNumber = StringUtils.remove(key, OPENSHIFT_CONTAINER_IMAGE);
        JSONObject containerConfig;
        containerConfig =
            new JSONObject(Templates.getInstance(taskContext).getContainersTemplate());
        containerConfig.put(
            Constants.IMAGE, taskContext.getConfigurationMap().get(Constants.NAME_U + key));
        containerConfig.put(
            Constants.NAME,
            taskContext.getConfigurationMap().get(OPENSHIFT_CONTAINER_NAME + elementNumber));
        containerConfig.put(Constants.PORTS, configurePorts(elementNumber));
        containerConfig.put(Constants.ENV, configureVars(elementNumber));
        containerConfig.put(
            Constants.VOLUME_MOUNTS,
            volumeMountListMap.get(
                taskContext.getConfigurationMap().get(OPENSHIFT_CONTAINER_NAME + elementNumber)));
        containers.put(new JSONObject((JSONObject.valueToString(containerConfig))));
      }
    }
    this.deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.TEMPLATE)
        .getJSONObject(Constants.SPEC)
        .put(Constants.CONTAINERS, containers);
  }

  private JSONArray configurePorts(String elementNumber) {
    JSONArray ports = new JSONArray();
    for (String port :
        StringUtils.split(
            StringUtils.replace(
                taskContext.getConfigurationMap().get(OPENSHIFT_CONTAINER_PORTS + elementNumber),
                SPACE,
                EMPTY_STRING),
            COMMA)) {
      JSONObject portsconfigJsonObject =
          new JSONObject(Templates.getInstance(taskContext).getPortsTemplate());
      String[] portsParts = StringUtils.split(port, Constants.SLASH);
      portsconfigJsonObject.put(Constants.CONTAINER_PORT, portsParts[0]);
      portsconfigJsonObject.put(Constants.PROTOCOL, portsParts[1].toUpperCase());
      ports.put(portsconfigJsonObject);
    }
    return ports;
  }

  private JSONArray configureVars(String elementNumber) {
    JSONArray vars = new JSONArray();
    for (String var :
        StringUtils.split(
            StringUtils.replace(
                taskContext.getConfigurationMap().get(OPENSHIFT_CONTAINER_VARS + elementNumber),
                Constants.SPACE,
                Constants.EMPTY_STRING),
            Constants.COMMA)) {
      JSONObject varsconfigJsonObject =
          new JSONObject(Templates.getInstance(taskContext).getEnvVarsTemplate());
      String[] varsParts = StringUtils.split(var, Constants.EQUALS);
      varsconfigJsonObject.put(Constants.NAME, varsParts[0]);
      varsconfigJsonObject.put(Constants.VALUE, varsParts[1].toUpperCase());
      vars.put(new JSONObject(JSONObject.valueToString(varsconfigJsonObject)));
    }
    return vars;
  }

  void setDeploymentStrategyRecreate() {
    this.deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.STRATEGY)
        .put(Constants.TYPE, Constants.RECREATE);
    this.deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.STRATEGY)
        .put(Constants.ROLLING_PARAMS, JSONObject.NULL);
  }

  void setDeploymentStrategyRolling() {
    this.deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.STRATEGY)
        .put(
            Constants.ROLLING_PARAMS,
            new JSONObject()
                .put(Constants.INTERVAL_SECONDS, 1)
                .put(Constants.MAX_SURGE, "25%")
                .put(Constants.MAX_UNAVAILABLE, "25%")
                .put(Constants.TIMEOUT_SECONDS, 600)
                .put(Constants.UPDATE_PERIOD_SECONDS, 1));
    this.deploymentConfigObject
        .getJSONObject(Constants.SPEC)
        .getJSONObject(Constants.STRATEGY)
        .put(Constants.TYPE, Constants.ROLLING);
  }

  void setReplicas(String replicas) {
    this.deploymentConfigObject.getJSONObject(Constants.SPEC).put(Constants.REPLICAS, replicas);
  }

  JSONObject getJsonObject() {
    return this.deploymentConfigObject;
  }
}
