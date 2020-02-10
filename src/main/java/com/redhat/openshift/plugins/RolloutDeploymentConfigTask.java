package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Objects;

public class RolloutDeploymentConfigTask implements DeploymentTaskType {

  /**
   * Executes task when a build plan is run. Gets deployment request template, populates it with
   * deploymentconfig name from configmap, then runs the request to create and poll deployment.
   *
   * @param taskContext the plugin task's context.
   * @return TaskResult to signal Bamboo of failed or successful build.
   */
  @Override
  @NotNull
  public TaskResult execute(@NotNull DeploymentTaskContext taskContext) {
    final BuildLogger buildLogger = taskContext.getBuildLogger();
    String ocpToken;
    String ocpServer;

    JSONObject deploymentRequestJsonObject =
        new JSONObject(Templates.getInstance(taskContext).getDeploymentRequestTemplate());

    deploymentRequestJsonObject.put(
        Constants.NAME,
        taskContext.getConfigurationMap().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING));

    try {
      Unirest.setHttpClient(Utils.setHttpClient());
    } catch (java.security.NoSuchAlgorithmException | java.security.KeyManagementException secEx) {
      Utils.sendLog(buildLogger, false, Constants.FAILED_TO_SET_UP_HTTP_CLIENT);
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
    try {
      ocpServer =
          taskContext
              .getDeploymentContext()
              .getVariableContext()
              .getEffectiveVariables()
              .get(BAMBOO_VAR_OCP_SERVER)
              .getValue();
      ocpToken =
          taskContext
              .getDeploymentContext()
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
    if (handleDeploymentStatus(
        taskContext,
        ocpToken,
        ocpServer,
        buildLogger,
        sendDeploymentRequest(
            taskContext, deploymentRequestJsonObject, buildLogger, ocpToken, ocpServer))) {
      return TaskResultBuilder.newBuilder(taskContext).success().build();
    } else {
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
  }

  /**
   * Sends deployment request to roll out the latest deploymentconfig. Uses HTTP POST to send the
   * request. If the response is not SC_CREATED, throws exception.
   *
   * @param deploymentTaskContext the plugin task context
   * @param deploymentRequestJsonObject the deployment request JSON to be sent
   * @param buildLogger buildlogger instance for writing to Bamboo log
   * @param token OpenShift token for authentication
   * @return JSONObject response containing the deploymentconfig, empty JSONObject if request is
   *     unsuccessful
   */
  private JSONObject sendDeploymentRequest(
      @NotNull DeploymentTaskContext deploymentTaskContext,
      @NotNull JSONObject deploymentRequestJsonObject,
      BuildLogger buildLogger,
      @NotNull String token,
      @NotNull String server) {
    // Send REST API POST to the OCP server, instantiate a build config
    try {
      Utils.sendLog(buildLogger, true, deploymentRequestJsonObject.toString());
      HttpResponse<JsonNode> jsonResponse =
          Unirest.post(
                  server
                      + Constants.OAPI_V_1_NAMESPACES
                      + deploymentTaskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                      + Constants.DEPLOYMENTCONFIGS
                      + deploymentTaskContext
                          .getConfigurationMap()
                          .get(OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING)
                      + Constants.INSTANTIATE)
              .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
              .header(Constants.AUTHORIZATION, Constants.BEARER + token)
              .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
              .body(deploymentRequestJsonObject.toString())
              .asJson();
      Utils.sendLog(buildLogger, jsonResponse);
      if (jsonResponse.getStatus() == SC_CREATED) {
        Utils.sendLog(
            buildLogger,
            false,
            Constants.DEPLOYMENT_SUCCESSFULLY_STARTED_WITH_REVISION
                + "("
                + jsonResponse
                    .getBody()
                    .getObject()
                    .getJSONObject(Constants.STATUS)
                    .get(Constants.LATEST_VERSION)
                    .toString()
                + ").");
        return jsonResponse.getBody().getObject();
      } else {
        throw new HttpException(jsonResponse.getStatus() + " " + jsonResponse.getStatusText());
      }
    } catch (UnirestException uniEx) {
      buildLogger.addErrorLogEntry(
          Constants.OPEN_SHIFT + Constants.ERROR_WHILE_STARTING_THE_DEPLOYMENT);
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + uniEx.toString());
    } catch (HttpException httpEx) {
      Utils.sendLog(
          buildLogger, false, Constants.REQUEST_FAILED + " (" + httpEx.getMessage() + ").");
    }
    return new JSONObject();
  }

  /**
   * Follows a deployment by polling the replication controller "deploymentconfigname-latestversion"
   * in the namespace and checking if readyReplicas are equal to replicas. Uses a HTTP GET to fetch
   * the replication controller. If the HTTP GET is not 200, retries 3 times.
   *
   * @param deploymentTaskContext plugin task context
   * @param token OpenShift token for authentication
   * @param buildLogger buildlogger instance for writing to Bamboo log
   * @param deploymentConfig deploymentConfig to use as reference
   * @return the result of handling the deployment. False if the deploymentconfig JSON is empty
   *     handling has failed. True if the deployment was successful.
   */
  private boolean handleDeploymentStatus(
      DeploymentTaskContext deploymentTaskContext,
      @NotNull String token,
      @NotNull String server,
      BuildLogger buildLogger,
      JSONObject deploymentConfig) {
    int retryNum = 0;
    int maxRetries = 3;
    HttpResponse<JsonNode> replicationControllerPollResponse;
    while (retryNum < maxRetries) {
      try {
        replicationControllerPollResponse =
            Unirest.get(
                    server
                        + Constants.API_V_1_NAMESPACES
                        + deploymentTaskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                        + Constants.REPLICATIONCONTROLLERS
                        + deploymentTaskContext
                            .getConfigurationMap()
                            .get(OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING)
                        + "-"
                        + deploymentConfig
                            .getJSONObject(Constants.STATUS)
                            .get(Constants.LATEST_VERSION))
                .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
                .header(Constants.AUTHORIZATION, Constants.BEARER + token)
                .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
                .asJson();
        if (replicationControllerPollResponse.getStatus() == SC_OK
            && !keepTrying(replicationControllerPollResponse)) {
          return true;
        } else if (replicationControllerPollResponse.getStatus() != SC_OK) {
          Utils.sendLog(buildLogger, false, replicationControllerPollResponse.getBody().toString());
          retryNum++;
        }
      } catch (UnirestException uniRest) {
        retryNum++;
        Utils.sendLog(buildLogger, false, uniRest.getMessage());
        Utils.sendLog(buildLogger, false, "Retrying...(" + retryNum + "/" + maxRetries + ")");
      }
    }
    return false;
  }

  private boolean keepTrying(HttpResponse<JsonNode> replicationControllerPollResponse) {
    return (replicationControllerPollResponse
            .getBody()
            .getObject()
            .getJSONObject(Constants.STATUS)
            .isNull(Constants.READY_REPLICAS)
        || !Objects.equals(
            replicationControllerPollResponse
                .getBody()
                .getObject()
                .getJSONObject(Constants.STATUS)
                .get(Constants.REPLICAS),
            replicationControllerPollResponse
                .getBody()
                .getObject()
                .getJSONObject(Constants.STATUS)
                .get(Constants.READY_REPLICAS)));
  }
}
