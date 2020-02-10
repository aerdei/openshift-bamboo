package com.redhat.openshift.plugins;

import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class Templates {

  private static Templates templatesSoleInstance;

  private String buildConfigTemplate;
  private String buildRequestTemplate;
  private String deploymentConfigTemplate;
  private String containersTemplate;
  private String portsTemplate;
  private String deploymentRequestTemplate;
  private String envVarsTemplate;
  private String emptyDirTemplate;
  private String persistentVolumeClaimTemplate;
  private String volumeMountTemplate;
  private String buildStrategyTemplate;
  private String buildSourceTemplate;

  private Templates(CommonTaskContext taskContext) {
    try {
      this.buildConfigTemplate = readFile("buildConfigTemplate.json");
      this.buildRequestTemplate = readFile("buildRequestTemplate.json");
      this.deploymentConfigTemplate = readFile("deploymentConfigTemplate.json");
      this.containersTemplate = readFile("containersTemplate.json");
      this.portsTemplate = readFile("portsTemplate.json");
      this.deploymentRequestTemplate = readFile("deploymentRequestTemplate.json");
      this.envVarsTemplate = readFile("envVarsTemplate.json");
      this.emptyDirTemplate = readFile("emptyDirTemplate.json");
      this.volumeMountTemplate = readFile("volumeMountTemplate.json");
      this.persistentVolumeClaimTemplate = readFile("persistentVolumeClaimTemplate.json");
      this.buildStrategyTemplate = readFile("buildStrategyTemplate.json");
      this.buildSourceTemplate = readFile("buildSourceTemplate.json");
    } catch (IOException ioE) {
      taskContext
          .getBuildLogger()
          .addErrorLogEntry(
              Constants.OPEN_SHIFT + " Could not generate templates.\n" + ioE.toString());
    }
  }

  static Templates getInstance(TaskContext taskContext) {
    if (templatesSoleInstance == null) {
      templatesSoleInstance = new Templates(taskContext);
    }
    return templatesSoleInstance;
  }

  static Templates getInstance(DeploymentTaskContext deploymentTaskContext) {
    if (templatesSoleInstance == null) {
      templatesSoleInstance = new Templates(deploymentTaskContext);
    }
    return templatesSoleInstance;
  }

  // Template getters
  String getBuildConfigTemplate() {
    return buildConfigTemplate;
  }

  String getBuildRequestTemplate() {
    return buildRequestTemplate;
  }

  String getDeploymentConfigTemplate() {
    return deploymentConfigTemplate;
  }

  String getContainersTemplate() {
    return containersTemplate;
  }

  String getPortsTemplate() {
    return portsTemplate;
  }

  String getDeploymentRequestTemplate() {
    return deploymentRequestTemplate;
  }

  String getEnvVarsTemplate() {
    return envVarsTemplate;
  }

  String getEmptyDirTemplate() {
    return emptyDirTemplate;
  }

  String getPersistentVolumeClaimTemplate() {
    return persistentVolumeClaimTemplate;
  }

  String getVolumeMountTemplate() {
    return volumeMountTemplate;
  }

  String getBuildStrategyTemplate() {
    return buildStrategyTemplate;
  }

  String getBuildSourceTemplate() {
    return buildSourceTemplate;
  }

  // File reader method
  private String readFile(String path) throws IOException {
    InputStream resource = Templates.class.getResourceAsStream("/templates/" + path);
    return IOUtils.toString(resource, StandardCharsets.UTF_8.toString());
  }
}
