package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.ARTIFACTORY_SERVER_TOKEN;
import static com.redhat.openshift.plugins.Constants.ARTIFACTORY_SERVER_URL;
import static com.redhat.openshift.plugins.Constants.AUTHORIZATION;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.CONTENT_TYPE;
import static com.redhat.openshift.plugins.Constants.FAILED_TO_SET_UP_HTTP_CLIENT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_TOKEN;
import static com.redhat.openshift.plugins.Constants.SPACE;
import static com.redhat.openshift.plugins.Constants.runningBuilds;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public class StartBuildTask implements TaskType {
  private String buildName;

  /**
   * Initiates the OpenShift build process
   *
   * @param taskContext TaskContext for the current plugin task. Used for retrieving task configs
   * @return successful task result if the build completes, failed task result otherwise
   */
  @Override
  @NotNull
  public TaskResult execute(@NotNull TaskContext taskContext) {

    final BuildLogger buildLogger = taskContext.getBuildLogger();
    String ocpToken;
    String ocpServer;
    String artifactoryUrl;
    String artifactoryToken;
    String lanternToken;

    try {
      lanternToken =
          taskContext
              .getBuildContext()
              .getVariableContext()
              .getOriginalVariables()
              .get("lantern_token")
              .getValue();
    } catch (NullPointerException npe) {
      lanternToken = Constants.EMPTY_STRING;
    }

    JSONObject buildRequestJsonObject =
        new JSONObject(Templates.getInstance(taskContext).getBuildRequestTemplate());

    buildRequestJsonObject
        .getJSONObject(Constants.METADATA)
        .put(Constants.NAME, taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME));

    buildRequestJsonObject.put(
        "env",
        new JSONArray()
            .put(
                new JSONObject(Templates.getInstance(taskContext).getEnvVarsTemplate())
                    .put(Constants.NAME, "bamboo_build_id")
                    .put(
                        Constants.VALUE,
                        taskContext.getBuildContext().getPlanResultKey().getPlanKey().toString()))
            .put(
                new JSONObject(Templates.getInstance(taskContext).getEnvVarsTemplate())
                    .put(Constants.NAME, "bamboo_build_number")
                    .put(
                        Constants.VALUE,
                        Integer.toString(
                            taskContext.getBuildContext().getPlanResultKey().getBuildNumber())))
            .put(
                new JSONObject(Templates.getInstance(taskContext).getEnvVarsTemplate())
                    .put(Constants.NAME, "lantern_token")
                    .put(Constants.VALUE, lanternToken)));

    // Set Unirest HTTP client to trust all certificates. Required temporarily to work with self
    // signed OCP certs.
    try {
      Unirest.setHttpClient(Utils.setHttpClient());
    } catch (java.security.NoSuchAlgorithmException | java.security.KeyManagementException secEx) {
      Utils.sendLog(buildLogger, false, FAILED_TO_SET_UP_HTTP_CLIENT);
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
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
    if (sendBuildRequest(taskContext, buildRequestJsonObject, buildLogger, ocpToken, ocpServer)) {
      try {
        artifactoryUrl =
            taskContext
                .getBuildContext()
                .getVariableContext()
                .getEffectiveVariables()
                .get(ARTIFACTORY_SERVER_URL)
                .getValue();
        artifactoryToken =
            taskContext
                .getBuildContext()
                .getVariableContext()
                .getEffectiveVariables()
                .get(ARTIFACTORY_SERVER_TOKEN)
                .getValue();
      } catch (NullPointerException npe) {
        artifactoryUrl = Constants.EMPTY_STRING;
        artifactoryToken = Constants.EMPTY_STRING;
      }
      if (!artifactoryUrl.isEmpty() && !artifactoryToken.isEmpty()) {
        downloadArtifacts(
            taskContext,
            buildLogger,
            getArtifactUrls(taskContext, buildLogger, artifactoryUrl, artifactoryToken));
      } else {
        Utils.sendLog(
            buildLogger,
            false,
            "Bamboo variable "
                + ARTIFACTORY_SERVER_URL
                + " or "
                + ARTIFACTORY_SERVER_TOKEN
                + " not provided. Build artifacts will not be downloaded.");
      }
      return TaskResultBuilder.newBuilder(taskContext).success().build();
    } else {
      return TaskResultBuilder.newBuilder(taskContext).failed().build();
    }
  }

  /**
   * Downloads files from a given URL to the current build's working directory. Each filename is the
   * artifactUrls key, URL is the artifactUrls value.
   *
   * @param taskContext TaskContext for the current plugin task. Used for getting the current
   *     directory.
   * @param buildLogger BuildLogger for the current build. Used for sending log messages.
   * @param artifactUrls a hashmap where key is the artifact filename, value is the download URL
   */
  private void downloadArtifacts(
      TaskContext taskContext, BuildLogger buildLogger, Map<String, String> artifactUrls) {
    Utils.sendLog(buildLogger, true, "RootDir: " + taskContext.getRootDirectory());
    Utils.sendLog(buildLogger, true, "WorkingDir: " + taskContext.getWorkingDirectory());
    if (!artifactUrls.isEmpty()) {
      for (Map.Entry<String, String> entry : artifactUrls.entrySet()) {
        try (FileOutputStream fos =
            new FileOutputStream(
                new File(taskContext.getWorkingDirectory().toString(), entry.getKey()))) {
          HttpResponse<InputStream> responseGet =
              Unirest.get(entry.getValue()).header(CONTENT_TYPE, "*/*").asBinary();
          if (responseGet.getStatus() == HttpsURLConnection.HTTP_OK) {
            while (responseGet.getBody().available() != 0) {
              fos.write(responseGet.getBody().read());
            }
          } else {
            Utils.sendLog(buildLogger, false, "Error while fetching artifacts form Artifactory.");
            Utils.sendLog(
                buildLogger,
                false,
                responseGet.getStatus()
                    + SPACE
                    + responseGet.getStatus()
                    + SPACE
                    + responseGet.getBody().toString());
          }
        } catch (UnirestException | IOException urex) {
          buildLogger.addErrorLogEntry(
              Constants.OPEN_SHIFT + Constants.ERROR_DOWNLOADING_THE_ARTIFACT);
          buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + urex.toString());
        }
      }
    } else {
      Utils.sendLog(buildLogger, false, "No artifacts found for this build.");
    }
  }

  /**
   * Queries Artifactory for artifacts using the URL artifactoryUrl and token artifactoryToken to
   * query for builds with Bamboo build name for name and Bamboo build number for number. Generates
   * URLs for the matched artifacts.
   *
   * @param taskContext TaskContext for the current plugin task. Used for retrieving task configs
   * @param buildLogger BuildLogger for the current build. Used for sending log messages
   * @param artifactoryUrl URL for Artifactory
   * @param artifactoryToken Token to authenticate with
   * @return a hashmap where key is the artifact name, value is the download URL
   */
  private Map<String, String> getArtifactUrls(
      TaskContext taskContext,
      BuildLogger buildLogger,
      String artifactoryUrl,
      String artifactoryToken) {
    Map<String, String> artifactUrls = new HashMap<>();
    try {
      HttpResponse<String> jsonResponse =
          Unirest.post(artifactoryUrl + "/api/search/aql")
              .header(Constants.CONTENT_TYPE, Constants.TEXT_PLAIN)
              .header(AUTHORIZATION, Constants.BEARER + artifactoryToken)
              .body(
                  "items.find({"
                      + "\"artifact.module.build.name\":{\"$eq\":\""
                      + taskContext.getBuildContext().getPlanResultKey().getPlanKey().toString()
                      + "\"},"
                      + "\"artifact.module.build.number\":{\"$eq\":\""
                      + taskContext.getBuildContext().getPlanResultKey().getBuildNumber()
                      + "\"}})"
                      + ".include(\"name\",\"repo\",\"path\")")
              .asString();
      Utils.sendLog(buildLogger, jsonResponse);
      if (jsonResponse.getStatus() != HttpsURLConnection.HTTP_OK) {
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT + Constants.ERROR_GETTING_THE_ARTIFACT_URLS);
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT + jsonResponse.getStatus() + " " + jsonResponse.getStatusText());
      } else {
        JSONArray results =
            new JSONArray(
                new JSONObject(jsonResponse.getBody()).getJSONArray("results").toString());
        for (int i = 0; i < results.length(); i++) {
          JSONObject result = new JSONObject(results.get(i).toString());
          artifactUrls.put(
              result.get(Constants.NAME).toString(),
              artifactoryUrl
                  .concat("/")
                  .concat(
                      result
                          .get("repo")
                          .toString()
                          .concat("/")
                          .concat(result.get("path").toString())
                          .concat("/")
                          .concat(result.get(Constants.NAME).toString())));
        }
      }
    } catch (UnirestException ex) {
      buildLogger.addErrorLogEntry(
          Constants.OPEN_SHIFT + Constants.ERROR_FETCHING_THE_ARTIFACT_URLS);
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + ex.toString());
    }
    return artifactUrls;
  }

  /**
   * Sends an HTTP POST to the OCP server to instantiate a build based on the plugin configuration.
   * Adds the build to the list of currently running builds and then handleBuildStatus to follow the
   * build until it ends with a result.
   *
   * @param taskContext TaskContext for the current plugin task. Used for retrieving task configs
   * @param buildRequestJsonObject a buildRequest object to be send at instantiation.
   * @param buildLogger BuildLogger for the current build. Used for sending log messages
   * @param token an OCP token for authentication
   * @param server an OCP server URL for comms
   * @return true if the build is successful
   */
  private boolean sendBuildRequest(
      @NotNull TaskContext taskContext,
      @NotNull JSONObject buildRequestJsonObject,
      BuildLogger buildLogger,
      @NotNull String token,
      @NotNull String server) {
    boolean result = false;
    // Send REST API POST to the OCP server, instantiate a build config
    try {
      HttpResponse<JsonNode> jsonResponse =
          Unirest.post(
                  server
                      + Constants.OAPI_V_1_NAMESPACES
                      + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                      + "/buildconfigs/"
                      + taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME)
                      + "/instantiate")
              .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
              .header(Constants.AUTHORIZATION, Constants.BEARER + token)
              .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
              .body(buildRequestJsonObject.toString())
              .asJson();
      Utils.sendLog(buildLogger, jsonResponse);
      if (jsonResponse.getStatus() == SC_CREATED) {
        buildName =
            new JSONObject(jsonResponse.getBody().toString())
                .getJSONObject(Constants.METADATA)
                .get(Constants.NAME)
                .toString();
        Utils.sendLog(buildLogger, true, "Build: " + buildName);
        Utils.sendLog(buildLogger, false, "Build successfully started " + "(" + buildName + ").");
        Utils.sendLog(
            buildLogger,
            false,
            Constants.BUILD_LOGS_AVAILABLE_AT
                + server
                + "/console/project/"
                + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                + "/browse/builds/"
                + taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME)
                + "/"
                + buildName
                + "?tab=logs");
        Map<String, String> buildMap = new HashMap<>();
        buildMap.put(OPENSHIFT_SERVER_ADDRESS, server);
        buildMap.put(
            OPENSHIFT_NAMESPACE, taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE));
        buildMap.put(
            OPENSHIFT_BUILDCONFIG_NAME,
            taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME));
        buildMap.put(OPENSHIFT_BUILD_NAME, buildName);
        buildMap.put(OPENSHIFT_TOKEN, token);
        runningBuilds.add(buildMap);
        result = handleBuildStatus(taskContext, token, server, buildLogger);
      } else {
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT
                + jsonResponse.getStatus()
                + jsonResponse.getStatusText()
                + jsonResponse.getBody());
        Unirest.shutdown();
      }
    } catch (UnirestException | java.io.IOException ex) {
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + Constants.ERROR_WHILE_STARTING_THE_BUILD);
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + ex.toString());
    }
    return result;
  }

  /**
   * Follows the build log based on the buildConfig settings in the task configuration. When the
   * build log stream is closed, it checks for the build phase. If the build phase is Running,
   * Pending, or New, it assumes that the build is still ongoing and tries to follow the log again.
   * If it is not, returns a boolean based on the phase.
   *
   * @param taskContext TaskContext for the current plugin task. Used for retrieving task configs
   * @param token an OCP token for authentication
   * @param server an OCP server URL for comms
   * @param buildLogger BuildLogger for the current build. Used for sending log messages
   * @return true if the build phase is "Complete"
   */
  private boolean handleBuildStatus(
      TaskContext taskContext,
      @NotNull String token,
      @NotNull String server,
      BuildLogger buildLogger) {
    boolean result = false;
    try {
      Unirest.get(
              server
                  + Constants.OAPI_V_1_NAMESPACES
                  + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                  + Constants.BUILDS
                  + buildName
                  + Constants.LOG_FOLLOW)
          .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
          .header(Constants.AUTHORIZATION, Constants.BEARER + token)
          .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
          .asString();
      HttpResponse<JsonNode> buildStatusPhase =
          Unirest.get(
                  server
                      + Constants.OAPI_V_1_NAMESPACES
                      + taskContext.getConfigurationMap().get(OPENSHIFT_NAMESPACE)
                      + Constants.BUILDS
                      + buildName)
              .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
              .header(Constants.AUTHORIZATION, Constants.BEARER + token)
              .header(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
              .asJson();
      JSONObject buildStatusPhaseJson = new JSONObject(buildStatusPhase.getBody().toString());
      String buildStatusPhaseString =
          buildStatusPhaseJson.getJSONObject(Constants.STATUS).get(Constants.PHASE).toString();
      if (!buildStatusPhaseJson.getJSONObject(Constants.STATUS).isNull(Constants.REASON)) {
        buildStatusPhaseString =
            buildStatusPhaseString.concat(
                "("
                    + buildStatusPhaseJson.getJSONObject(Constants.STATUS).get(Constants.REASON)
                    + ")");
      }
      if (Objects.equals(buildStatusPhaseString, Constants.COMPLETE)) {
        Utils.sendLog(
            buildLogger, false, Constants.BUILD_FINISHED_STATUS + buildStatusPhaseString + ".");
        result = true;
      } else if (Objects.equals(buildStatusPhaseString, Constants.RUNNING)
          || Objects.equals(buildStatusPhaseString, Constants.PENDING)
          || Objects.equals(buildStatusPhaseString, Constants.NEW)) {
        Thread.sleep(1000);
        result = handleBuildStatus(taskContext, token, server, buildLogger);
      } else {
        buildLogger.addErrorLogEntry(
            Constants.OPEN_SHIFT
                + Constants.BUILD_FINISHED_STATUS
                + buildStatusPhaseString
                + "\".");
      }
    } catch (UnirestException | InterruptedException ex) {
      buildLogger.addErrorLogEntry(
          Constants.OPEN_SHIFT + Constants.AN_ERROR_OCCURED + ex.toString());
      Thread.currentThread().interrupt();
    }
    return result;
  }
}
