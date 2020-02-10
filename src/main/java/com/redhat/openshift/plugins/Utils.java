package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.EMPTY_STRING;
import static com.redhat.openshift.plugins.Constants.FIELD_EMPTY_ERROR;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_DOCKER_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_IMAGESTREAM_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_TYPE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_PW;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_USERNAME;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanIdentifier;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.cache.CachedPlanManager;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.task.CommonTaskContext;
import com.atlassian.bamboo.task.TaskContextHelper;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.VariableDefinition;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

final class Utils {

  private Utils() {
    throw new IllegalStateException("Utility class");
  }

  static String getToken(@NotNull CommonTaskContext taskContext, BuildLogger buildLogger) {
    sendLog(buildLogger, false, "Requesting token from OpenShift.");
    // Get a token from OCP using user and pass
    String token;
    try {
      HttpResponse<String> jsonResponseAuth =
          Unirest.get(
                  taskContext.getConfigurationMap().get(OPENSHIFT_SERVER_ADDRESS)
                      + "/oauth/authorize?response_type=token&"
                      + "client_id=openshift-challenging-client")
              .basicAuth(
                  taskContext.getConfigurationMap().get(OPENSHIFT_USERNAME),
                  taskContext.getConfigurationMap().get(OPENSHIFT_PW))
              .asString();
      if (jsonResponseAuth.getStatus() == SC_MOVED_TEMPORARILY) {
        sendLog(buildLogger, jsonResponseAuth);
        token =
            jsonResponseAuth.getBody()
                + StringUtils.substringBefore(
                    StringUtils.substringAfter(jsonResponseAuth.getHeaders().toString(), "token="),
                    "&expires_in");
        return token;

      } else {
        throw new UnirestException(
            jsonResponseAuth.getStatus()
                + jsonResponseAuth.getStatusText()
                + jsonResponseAuth.getBody());
      }
    } catch (UnirestException uniEx) {
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + " Error while requesting token. ");
      buildLogger.addErrorLogEntry(Constants.OPEN_SHIFT + uniEx.toString());
      return Constants.EMPTY_STRING;
    }
  }

  static void sendLog(BuildLogger buildLogger, HttpResponse httpResponse) {
    String logMessage =
        Constants.OPEN_SHIFT
            + " [Debug] "
            + httpResponse.getStatus()
            + httpResponse.getStatusText()
            + httpResponse.getBody()
            + httpResponse.getHeaders();
    buildLogger.addBuildLogEntry(logMessage);
  }

  static void sendLog(BuildLogger buildLogger, boolean debug, String message) {
    if (debug) {
      buildLogger.addBuildLogEntry(Constants.OPEN_SHIFT + " [Debug] " + message);
    } else {
      buildLogger.addBuildLogEntry(Constants.OPEN_SHIFT + message);
    }
  }

  /**
   * Creates a CloseableHttpClient that trusts self-signed certificates and has redirecting
   * disabled. Former for compatibility with self-signed OCP certificates, latter for the ability of
   * retrieving an OCP token.
   *
   * @return a CloseableHttpClient with the options set
   * @throws java.security.NoSuchAlgorithmException if no Provider supports a TrustManagerFactorySpi
   *     implementation for the specified protocol
   * @throws java.security.KeyManagementException if initiation fails
   */
  static CloseableHttpClient setHttpClient()
      throws java.security.NoSuchAlgorithmException, java.security.KeyManagementException {
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
              /*Implementation intentionally left blank*/
              if (authType.isEmpty()) {
                throw new CertificateException();
              }
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
              /*Implementation intentionally left blank*/
              if (authType.isEmpty()) {
                throw new CertificateException();
              }
            }
          }
        };
    SSLContext sslcontext = SSLContext.getInstance("TLSv1.2");
    sslcontext.init(null, trustAllCerts, new SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
    return HttpClients.custom().setSSLSocketFactory(sslsf).disableRedirectHandling().build();
  }

  /**
   * Returns a list of plan variables associated with the task.
   *
   * @param context context of the task
   * @return A List of VariableDefinitions from the plan
   */
  static List<VariableDefinition> getPlanVariables(
      Map<String, Object> context,
      CachedPlanManager cachedPlanManager,
      PlanManager planManager,
      VariableDefinitionManager variableDefinitionManager) {
    ImmutablePlan immutablePlan;
    List<VariableDefinition> planVars = Lists.newArrayList();
    if (TaskContextHelper.isDeploymentMode(context)) {
      immutablePlan = TaskContextHelper.getRelatedPlan(context);
      planVars = variableDefinitionManager.getPlanVariables(immutablePlan);
    } else {
      immutablePlan = TaskContextHelper.getPlan(context);
      PlanKey currentPlanKey;
      Plan exactPlan = null;
      if (immutablePlan != null) {
        currentPlanKey = immutablePlan.getPlanKey();
        String planKeyString =
            StringUtils.removeEnd(currentPlanKey.getKey(), "-" + currentPlanKey.getPartialKey());
        Optional<ImmutableChain> anyPlan =
            cachedPlanManager.getAnyPlan(
                p -> Objects.equals(p.getPlanKey().toString(), planKeyString));
        if (anyPlan.isPresent()) {
          exactPlan = planManager.getPlanByKey(anyPlan.get().getPlanKey());
        }
        if (exactPlan != null) {
          planVars = variableDefinitionManager.getPlanVariables(exactPlan);
        }
      }
    }
    return planVars;
  }

  private static List<VariableDefinition> getBambooPlanVariables(
      Map<String, Object> context,
      VariableDefinitionManager variableDefinitionManager,
      PlanManager planManager) {
    ImmutablePlan plan = TaskContextHelper.getPlan(context);
    if (plan != null) {
      PlanIdentifier planId =
          planManager.getPlanIdentifierForPermissionCheckingByKey(plan.getKey());
      if (planId != null) {
        return variableDefinitionManager.getPlanVariables(planId);
      }
    }
    return new ArrayList<>();
  }

  static void setupBambooVariables(
      List<String> variables,
      Map<String, Object> context,
      VariableDefinitionManager variableDefinitionManager,
      PlanManager planManager) {
    List<VariableDefinition> planVars =
        Utils.getBambooPlanVariables(context, variableDefinitionManager, planManager);
    variables.forEach(
        varName ->
            context.put(
                varName,
                planVars
                    .parallelStream()
                    .filter(var -> var.getKey().equals(varName))
                    .map(VariableDefinition::getValue)
                    .findFirst()
                    .orElse("")));
  }

  /**
   * Returns the first match of variableName from the variables to be used for plugin comms.
   *
   * @param variableName name of the variable to search for
   * @param planVariables list of plan variables
   * @return the first match to "variableName" in the list of VariableDefinitions as a String
   */
  static String getBambooVariable(String variableName, List<VariableDefinition> planVariables) {
    List<VariableDefinition> bambooVars =
        planVariables.stream()
            .filter(variableDefinition -> Objects.equals(variableDefinition.getKey(), variableName))
            .collect(Collectors.toList());
    if (bambooVars.isEmpty()) {
      return Constants.EMPTY_STRING;
    } else {
      return bambooVars.get(0).getValue();
    }
  }

  static List<ConfigElement> getBuildConfigElements(@NotNull final Map<String, Object> context) {
    List<ConfigElement> bcElements = Lists.newArrayList();
    ImmutablePlan relatedPlan = TaskContextHelper.getPlan(context);
    BuildDefinition buildDefinition;
    if (relatedPlan != null) {
      buildDefinition = relatedPlan.getBuildDefinition();
      for (TaskDefinition task : buildDefinition.getTaskDefinitions()) {
        if (Objects.equals(task.getPluginKey(), Constants.BC_PLUGIN_KEY)
            && task.isEnabled()
            && task.getConfiguration().containsKey(OPENSHIFT_BUILDCONFIG_NAME)) {
          String buildConfigOutputName;
          if (Objects.equals(
              task.getConfiguration().get(OPENSHIFT_OUTPUT_TYPE), Constants.DOCKER_REPO)) {
            buildConfigOutputName = task.getConfiguration().get(OPENSHIFT_OUTPUT_DOCKER_NAME);
          } else {
            buildConfigOutputName = task.getConfiguration().get(OPENSHIFT_OUTPUT_IMAGESTREAM_NAME);
          }
          bcElements.add(
              ConfigElement.createBuildConfigElement(
                  task.getId(),
                  task.getUserDescription(),
                  task.getConfiguration().get(OPENSHIFT_BUILDCONFIG_NAME),
                  task.getConfiguration().get(OPENSHIFT_NAMESPACE),
                  buildConfigOutputName));
        }
      }
    }
    return bcElements;
  }

  static void validateCommonAuthParams(
      @NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
    if (StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_TOKEN))
        || StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_SERVER))) {
      Arrays.asList(OPENSHIFT_SERVER_ADDRESS, OPENSHIFT_USERNAME, OPENSHIFT_PW)
          .parallelStream()
          .filter(field -> StringUtils.isEmpty(params.getString(field, EMPTY_STRING)))
          .collect(Collectors.toList())
          .forEach(f -> errorCollection.addError(f, FIELD_EMPTY_ERROR));
    }
  }
}
