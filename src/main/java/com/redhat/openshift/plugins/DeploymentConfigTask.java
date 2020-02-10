package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.FAILED_TO_SET_UP_HTTP_CLIENT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_EXISTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENT_REPLICAS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENT_STRATEGY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Objects;

public class DeploymentConfigTask implements TaskType {

  @Override
  @NotNull
  public TaskResult execute(@NotNull TaskContext taskContext) {
    final BuildLogger buildLogger = taskContext.getBuildLogger();
    String ocpToken;
    String ocpServer;
    // Set Unirest HTTP client to trust all certificates. Required temporarily to work with self
    // signed OCP certs.
    try {
      Unirest.setHttpClient(Utils.setHttpClient());
    } catch (java.security.NoSuchAlgorithmException | java.security.KeyManagementException secEx) {
      Utils.sendLog(buildLogger, false, FAILED_TO_SET_UP_HTTP_CLIENT);
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }

    DeploymentConfig deploymentConfig = new DeploymentConfig(taskContext);

    deploymentConfig.configureContainers();
    deploymentConfig.setReplicas(
        taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENT_REPLICAS));

    if (Objects.equals(
        taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENT_STRATEGY),
        Constants.OCP_DEPLOYMENT_STRATEGY_RECREATE)) {
      deploymentConfig.setDeploymentStrategyRecreate();
    } else if (Objects.equals(
        taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENT_STRATEGY),
        Constants.OCP_DEPLOYMENT_STRATEGY_ROLLING)) {
      deploymentConfig.setDeploymentStrategyRolling();
    }
    try {
      ocpServer =
          taskContext
              .getBuildContext()
              .getVariableContext()
              .getEffectiveVariables()
              .get(BAMBOO_VAR_OCP_SERVER)
              .getValue();
      ocpToken =
          taskContext
              .getBuildContext()
              .getVariableContext()
              .getEffectiveVariables()
              .get(BAMBOO_VAR_OCP_TOKEN)
              .getValue();
    } catch (NullPointerException npe) {
      ocpServer = Constants.EMPTY_STRING;
      ocpToken = Constants.EMPTY_STRING;
    }
    if (ocpServer.isEmpty() || ocpToken.isEmpty()) {
      ocpToken = Utils.getToken(taskContext, buildLogger);
      ocpServer = taskContext.getConfigurationMap().get(OPENSHIFT_SERVER_ADDRESS);
    }
    if (sendJsonRequest(
        taskContext, deploymentConfig.getJsonObject(), buildLogger, ocpToken, ocpServer)) {
      return TaskResultBuilder.newBuilder(taskContext).success().build();
    } else {
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
  }

  private boolean sendJsonRequest(
      @NotNull TaskContext taskContext,
      @NotNull JSONObject deploymentconfigJsonObject,
      BuildLogger buildLogger,
      @NotNull String token,
      @NotNull String server) {
    boolean result = false;

    // Send REST API POST to the OCP server, create a deployment config
    try {

      HttpResponse<JsonNode> jsonResponse =
          Unirest.post(
                  server
                      + Constants.OAPI_V_1_NAMESPACES
                      + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                      + "/deploymentconfigs/")
              .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
              .header(Constants.AUTHORIZATION, Constants.BEARER + token)
              .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
              .body(deploymentconfigJsonObject.toString())
              .asJson();
      Utils.sendLog(buildLogger, jsonResponse);
      if (jsonResponse.getStatus() == SC_CREATED) {
        Utils.sendLog(buildLogger, false, "DeploymentConfig successfully created.");
        result = true;
      } else if (jsonResponse.getStatus() == SC_CONFLICT) {
        if (Objects.equals(
            taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS),
            Constants.BAMBOO_DC_EXISTS_OVERWRITE)) {
          Utils.sendLog(
              buildLogger,
              true,
              "spec:" + deploymentconfigJsonObject.getJSONObject(Constants.SPEC).toString());
          HttpResponse<JsonNode> jsonResponsePatch =
              Unirest.patch(
                      server
                          + Constants.OAPI_V_1_NAMESPACES
                          + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                          + "/deploymentconfigs/"
                          + taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME))
                  .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
                  .header(Constants.AUTHORIZATION, Constants.BEARER + token)
                  .header(Constants.CONTENT_TYPE, "application/merge-patch+json")
                  .body(
                      new JSONObject()
                          .put(
                              Constants.SPEC,
                              deploymentconfigJsonObject.getJSONObject(Constants.SPEC))
                          .toString())
                  .asJson();
          Utils.sendLog(buildLogger, jsonResponsePatch);
          result = true;
        } else if (Objects.equals(
            taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS),
            Constants.BAMBOO_DC_EXISTS_IGNORE)) {
          result = true;
        }
      } else {
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT + " Error while creating the deployment config.");
        buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + jsonResponse.getBody().toString());
      }

    } catch (UnirestException uniEx) {
      buildLogger.addErrorLogEntry(
          Constants.OPEN_SHIFT + " Error while creating the deployment config.");
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + uniEx.getMessage());
    }
    return result;
  }
}
