package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.ARTIFACTORY_SERVER_TOKEN;
import static com.redhat.openshift.plugins.Constants.ARTIFACTORY_SERVER_URL;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIGS_LIST;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_PW;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_USERNAME;
import static com.redhat.openshift.plugins.Utils.getBuildConfigElements;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StartBuildTaskConfigurator extends AbstractTaskConfigurator {

  @ComponentImport private final VariableDefinitionManager variableDefinitionManager;
  @ComponentImport private final PlanManager planManager;
  private List<ConfigElement> buildConfigElements;

  public StartBuildTaskConfigurator(
      @NotNull final VariableDefinitionManager variableDefinitionManager,
      @NotNull final PlanManager planManager) {
    this.variableDefinitionManager = variableDefinitionManager;
    this.planManager = planManager;
  }

  @Override
  public void validate(
      @NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
    super.validate(params, errorCollection);
    Utils.validateCommonAuthParams(params, errorCollection);
  }

  @Override
  @NotNull
  public Map<String, String> generateTaskConfigMap(
      @NotNull final ActionParametersMap params,
      @Nullable final TaskDefinition previousTaskDefinition) {
    final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
    Arrays.asList(
            OPENSHIFT_SERVER_ADDRESS,
            OPENSHIFT_USERNAME,
            OPENSHIFT_PW,
            BAMBOO_VAR_OCP_TOKEN,
            BAMBOO_VAR_OCP_SERVER)
        .forEach(field -> config.put(field, params.getString(field)));
    String bcNameNumString = params.getString(OPENSHIFT_BUILDCONFIG_NAME);
    buildConfigElements
        .parallelStream()
        .filter(element -> Objects.equals(Long.toString(element.getId()), bcNameNumString))
        .findFirst()
        .ifPresent(
            elem -> {
              config.put(OPENSHIFT_NAMESPACE, elem.getConfigNamespace());
              config.put(OPENSHIFT_BUILDCONFIG_NAME, elem.getConfigName());
            });
    return config;
  }

  @Override
  public void populateContextForCreate(@NotNull final Map<String, Object> context) {
    super.populateContextForCreate(context);
    Utils.setupBambooVariables(
        Arrays.asList(
            BAMBOO_VAR_OCP_TOKEN,
            BAMBOO_VAR_OCP_SERVER,
            ARTIFACTORY_SERVER_URL,
            ARTIFACTORY_SERVER_TOKEN),
        context,
        variableDefinitionManager,
        planManager);
    context.put(OPENSHIFT_SERVER_ADDRESS, "https://192.168.42.237:8443");
    context.put(OPENSHIFT_USERNAME, "admin");
    buildConfigElements = getBuildConfigElements(context);
    context.put(OPENSHIFT_BUILDCONFIGS_LIST, buildConfigElements);
  }

  @Override
  public void populateContextForEdit(
      @NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
    super.populateContextForEdit(context, taskDefinition);
    Utils.setupBambooVariables(
        Arrays.asList(
            BAMBOO_VAR_OCP_TOKEN,
            BAMBOO_VAR_OCP_SERVER,
            ARTIFACTORY_SERVER_URL,
            ARTIFACTORY_SERVER_TOKEN),
        context,
        variableDefinitionManager,
        planManager);
    Arrays.asList(OPENSHIFT_SERVER_ADDRESS, OPENSHIFT_USERNAME, OPENSHIFT_PW, OPENSHIFT_NAMESPACE)
        .forEach(field -> context.put(field, taskDefinition.getConfiguration().get(field)));
    buildConfigElements = getBuildConfigElements(context);
    context.put(OPENSHIFT_BUILDCONFIGS_LIST, buildConfigElements);
  }
}
