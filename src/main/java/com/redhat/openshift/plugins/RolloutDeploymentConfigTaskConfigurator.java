package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIGS_LIST;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_PW;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_USERNAME;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plan.cache.ImmutableJob;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskContextHelper;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RolloutDeploymentConfigTaskConfigurator extends AbstractTaskConfigurator {

  @ComponentImport private final VariableDefinitionManager variableDefinitionManager;
  @ComponentImport private final PlanManager planManager;
  private List<ConfigElement> deploymentConfigElements;

  public RolloutDeploymentConfigTaskConfigurator(
      @NotNull final VariableDefinitionManager variableDefinitionManager,
      @NotNull final PlanManager planManager) {
    this.variableDefinitionManager = variableDefinitionManager;
    this.planManager = planManager;
  }

  @Override
  public void populateContextForCreate(@NotNull final Map<String, Object> context) {
    super.populateContextForCreate(context);
    Utils.setupBambooVariables(
        Arrays.asList(BAMBOO_VAR_OCP_TOKEN, BAMBOO_VAR_OCP_SERVER),
        context,
        variableDefinitionManager,
        planManager);
    context.put(OPENSHIFT_SERVER_ADDRESS, "https://192.168.42.237:8443");
    context.put(OPENSHIFT_USERNAME, "admin");
    deploymentConfigElements = getDeploymentConfigElements(context);
    context.put(OPENSHIFT_DEPLOYMENTCONFIGS_LIST, deploymentConfigElements);
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
            BAMBOO_VAR_OCP_SERVER,
            OPENSHIFT_DEPLOYMENTCONFIG_NAME)
        .forEach(field -> config.put(field, params.getString(field)));
    String elemConfigName;
    for (ConfigElement elem : deploymentConfigElements) {
      elemConfigName = params.getString(OPENSHIFT_DEPLOYMENTCONFIG_NAME);
      if (elemConfigName != null && elem.id == Integer.parseInt(elemConfigName)) {
        config.put(OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING, elem.getConfigName());
        config.put(OPENSHIFT_NAMESPACE, elem.getConfigNamespace());
      }
    }
    return config;
  }

  @Override
  public void populateContextForEdit(
      @NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
    super.populateContextForEdit(context, taskDefinition);
    Utils.setupBambooVariables(
        Arrays.asList(BAMBOO_VAR_OCP_TOKEN, BAMBOO_VAR_OCP_SERVER),
        context,
        variableDefinitionManager,
        planManager);
    context.put(
        OPENSHIFT_SERVER_ADDRESS, taskDefinition.getConfiguration().get(OPENSHIFT_SERVER_ADDRESS));
    context.put(OPENSHIFT_USERNAME, taskDefinition.getConfiguration().get(OPENSHIFT_USERNAME));
    deploymentConfigElements = getDeploymentConfigElements(context);
    context.put(OPENSHIFT_DEPLOYMENTCONFIGS_LIST, deploymentConfigElements);
  }

  private List<ConfigElement> getDeploymentConfigElements(
      @NotNull final Map<String, Object> context) {
    List<ConfigElement> deploymentConfigs = Lists.newArrayList();
    ImmutablePlan relatedPlan = TaskContextHelper.getRelatedPlan(context);
    if (relatedPlan != null) {
      for (ImmutableJob job : ((ImmutableChain) relatedPlan).getAllJobs()) {
        for (TaskDefinition task : job.getTaskDefinitions()) {
          if (Objects.equals(task.getPluginKey(), Constants.DC_PLUGIN_KEY)
              && task.isEnabled()
              && task.getConfiguration().containsKey(OPENSHIFT_DEPLOYMENTCONFIG_NAME)) {
            deploymentConfigs.add(
                ConfigElement.createDeploymentConfigElement(
                    task.getId(),
                    task.getUserDescription(),
                    task.getConfiguration().get(OPENSHIFT_DEPLOYMENTCONFIG_NAME),
                    task.getConfiguration().get(OPENSHIFT_NAMESPACE)));
          }
        }
      }
    }
    return deploymentConfigs;
  }
}
