package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.ARTIFACT_LOCATION;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_EXISTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_ENV_VARS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_STRATEGY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_DOCKER_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_IMAGESTREAM_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_TYPE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_DOCKERFILE_GIT;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_SOURCE_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_DOCKERFILE_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_SOURCE_GIT;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

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

/**
 * This class is used for creating buildconfigs in OpenShift. It consists of methods used for
 * building a JSON object from template and sending it to the cluster for creation.
 *
 * @author Attila Erdei
 */
public class BuildConfigTask implements TaskType {

  /**
   * Populates a JSONObject from the buildConfig template using the configurations stored in the
   * taskContext configuration map. The JSONObject creation is influenced by configuration stored
   * previously when setting up the task.
   *
   * @param taskContext the context instance of the plugin task
   * @return a TaskResult, success upon the creation of the buildconfig, or fail otherwise
   */
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
      Utils.sendLog(buildLogger, false, Constants.FAILED_TO_SET_UP_HTTP_CLIENT);
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }

    Utils.sendLog(buildLogger, false, Constants.CREATING_TEMPLATE_JSON);
    BuildConfig buildConfig = new BuildConfig(taskContext);
    switch (taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_STRATEGY)) {
      case Constants.OCP_BUILD_SOURCE:
        buildConfig.setGitSource(
            taskContext.getConfigurationMap().get(REPOSITORY_ADDRESS_SOURCE_GIT));
        buildConfig.setSourceStrategy(
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_GIT),
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_TYPE_GIT),
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_ENV_VARS));
        buildConfig.setSourceSecret(
            taskContext.getConfigurationMap().get(SOURCE_SECRET_SOURCE_GIT));
        Utils.sendLog(buildLogger, true, buildConfig.getJsonObject().toString());
        break;

      case Constants.OCP_BUILD_ARTIFACT:
        switch (taskContext.getConfigurationMap().get(ARTIFACT_LOCATION)) {
          case Constants.OCP_BUILD_BAMBOO_ARTIFACT:
            buildConfig.setBinarySource();
            buildConfig.setSourceStrategy(
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_ENV_VARS));
            Utils.sendLog(buildLogger, true, buildConfig.getJsonObject().toString());
            break;

          case Constants.OCP_BUILD_GIT:
            buildConfig.setSourceStrategy(
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_ENV_VARS));
            buildConfig.setGitSource(
                taskContext.getConfigurationMap().get(REPOSITORY_ADDRESS_ARTIFACT_GIT));
            buildConfig.setSourceSecret(
                taskContext.getConfigurationMap().get(SOURCE_SECRET_ARTIFACT_GIT));
            Utils.sendLog(buildLogger, true, buildConfig.getJsonObject().toString());
            break;

          case Constants.OCP_BUILD_ARTIFACTORY:
            buildConfig.setSourceStrategy(
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY),
                taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_ENV_VARS));
            buildConfig.setBinarySource();
            Utils.sendLog(buildLogger, true, buildConfig.getJsonObject().toString());
            break;

          default:
            TaskResultBuilder.newBuilder(taskContext).failed().build();
            break;
        }
        break;

      case Constants.OCP_BUILD_DOCKERFILE:
        buildConfig.setDockerStrategy(
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE),
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE),
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILD_ENV_VARS));
        buildConfig.setGitSource(
            taskContext.getConfigurationMap().get(REPOSITORY_ADDRESS_DOCKERFILE_GIT));
        buildConfig.setSourceSecret(
            taskContext.getConfigurationMap().get(SOURCE_SECRET_DOCKERFILE_GIT));
        Utils.sendLog(buildLogger, true, buildConfig.getJsonObject().toString());
        break;

      default:
        TaskResultBuilder.newBuilder(taskContext).failed().build();
        break;
    }

    switch (taskContext.getConfigurationMap().get(OPENSHIFT_OUTPUT_TYPE)) {
      case Constants.OCP_OUTPUT_IMAGESTREAM:
        buildConfig.setImageOutputImageStream(
            taskContext.getConfigurationMap().get(OPENSHIFT_OUTPUT_IMAGESTREAM_NAME));
        break;

      case Constants.OCP_OUTPUT_DOCKER_REPO:
        buildConfig.setImageOutputDockerImage(
            taskContext.getConfigurationMap().get(OPENSHIFT_OUTPUT_DOCKER_NAME));
        break;

      default:
        TaskResultBuilder.newBuilder(taskContext).failed().build();
        break;
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
        taskContext, buildConfig.getJsonObject(), buildLogger, ocpToken, ocpServer)) {
      return TaskResultBuilder.newBuilder(taskContext).success().build();
    } else {
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
  }

  /**
   * Sends a HTTP POST request to the cluster defined in the task configuration map as
   * OPENSHIFT_SERVER_ADDRESS and namespace defined as OPENSHIFT_NAMESPACE to create a buildConfig.
   *
   * @param taskContext the context instance of the plugin task
   * @param buildConfigJsonObject the JSON object to send as buildconfig
   * @param buildLogger instance of the build logger to send logs to the user
   * @param token an OpenShift token for authentication
   */
  private Boolean sendJsonRequest(
      @NotNull TaskContext taskContext,
      @NotNull JSONObject buildConfigJsonObject,
      BuildLogger buildLogger,
      @NotNull String token,
      @NotNull String server) {
    boolean result = false;

    try {
      HttpResponse<JsonNode> jsonResponse =
          Unirest.post(
                  server
                      + Constants.OAPI_V_1_NAMESPACES
                      + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                      + Constants.BUILDCONFIGS)
              .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
              .header(Constants.AUTHORIZATION, Constants.BEARER + token)
              .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
              .body(buildConfigJsonObject.toString())
              .asJson();

      Utils.sendLog(buildLogger, jsonResponse);
      if (jsonResponse.getStatus() == SC_CREATED) {
        Utils.sendLog(buildLogger, false, Constants.BUILD_CONFIG_SUCCESSFULLY_CREATED);
        result = true;
      } else if (jsonResponse.getStatus() == SC_CONFLICT) {
        Utils.sendLog(buildLogger, false, Constants.BUILD_CONFIG_ALREADY_EXISTS);

        switch (taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_EXISTS)) {
          case Constants.BAMBOO_BC_EXISTS_IGNORE:
            Utils.sendLog(buildLogger, false, Constants.IGNORING_NEW_BUILD_CONFIG_SETTINGS);
            result = true;
            break;

          case Constants.BAMBOO_BC_EXISTS_FAIL:
            Utils.sendLog(buildLogger, false, Constants.HALTING_BUILD_CONFIG_CREATION);
            break;

          case Constants.BAMBOO_BC_EXISTS_OVERWRITE:
            Utils.sendLog(buildLogger, false, Constants.EXISTING_BUILD_CONFIG_WILL_BE_OVERWRITTEN);
            Utils.sendLog(buildLogger, false, Constants.PATCHING_EXISTING_BUILD_CONFIG);
            HttpResponse<JsonNode> jsonResponsePatch =
                Unirest.patch(
                        server
                            + Constants.OAPI_V_1_NAMESPACES
                            + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                            + Constants.BUILDCONFIGS
                            + buildConfigJsonObject
                                .getJSONObject(Constants.METADATA)
                                .get(Constants.NAME))
                    .header(Constants.ACCEPT, Constants.APPLICATION_JSON)
                    .header(Constants.AUTHORIZATION, Constants.BEARER + token)
                    .header(Constants.CONTENT_TYPE, Constants.APPLICATION_MERGE_PATCH_JSON)
                    .body(new JSONObject(buildConfigJsonObject, new String[] {Constants.SPEC}))
                    .asJson();
            Utils.sendLog(buildLogger, jsonResponsePatch);
            result = (jsonResponsePatch.getStatus() == SC_OK);
            break;

          default:
            break;
        }
      } else {
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT + Constants.ERROR_WHILE_CREATING_THE_BUILD_CONFIG);
        buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + jsonResponse.getBody().toString());
      }

    } catch (UnirestException uniEx) {
      buildLogger.addErrorLogEntry(
          Constants.OPEN_SHIFT + Constants.ERROR_WHILE_CREATING_THE_BUILD_CONFIG);
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + uniEx.toString());
    }
    return result;
  }
}
