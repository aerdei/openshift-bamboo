package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.DEPLOYMENTCONFIG_NAME_PATTERN;
import static com.redhat.openshift.plugins.Constants.EMPTY_STRING;
import static com.redhat.openshift.plugins.Constants.FIELD_EMPTY_ERROR;
import static com.redhat.openshift.plugins.Constants.INVALID_CONTAINER_NAME;
import static com.redhat.openshift.plugins.Constants.INVALID_DEPLOYMENTCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.INVALID_NAMESPACE_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIGS_LIST;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_IMAGE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_PORTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_MOUNT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_PVC_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_STORAGE_TYPE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_CONTAINER_VARS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_EXISTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_EXISTS_STRATEGIES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENTCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENT_REPLICAS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENT_STRATEGIES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_DEPLOYMENT_STRATEGY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE_CONTAINER_PATTERN;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_PW;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_STORAGETYPE_LIST;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_USERNAME;
import static com.redhat.openshift.plugins.Constants.PORTS_PATTERN;
import static com.redhat.openshift.plugins.Constants.SPACE;
import static com.redhat.openshift.plugins.Utils.getBuildConfigElements;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeploymentConfigConfigurator extends AbstractTaskConfigurator {

  private final PlanManager planManager;
  private final VariableDefinitionManager variableDefinitionManager;

  private List<ConfigElement> buildConfigElements;

  @Autowired
  public DeploymentConfigConfigurator(
      @ComponentImport final PlanManager planManager,
      @ComponentImport final VariableDefinitionManager variableDefinitionmanager) {
    this.planManager = planManager;
    this.variableDefinitionManager = variableDefinitionmanager;
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
    context.put(OPENSHIFT_NAMESPACE, "bamboo-namespace");
    context.put(OPENSHIFT_DEPLOYMENT_REPLICAS, 1);
    context.put(OPENSHIFT_DEPLOYMENT_STRATEGY, "rolling");
    context.put(OPENSHIFT_DEPLOYMENT_STRATEGIES, getDeploymentStrategies());
    context.put("containerList", Lists.newArrayList(0));
    buildConfigElements = getBuildConfigElements(context);
    context.put(OPENSHIFT_BUILDCONFIGS_LIST, buildConfigElements);
    context.put(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS, "overwrite");
    context.put(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS_STRATEGIES, getDeploymentConfigExistsMap());
    context.put("storageList", Lists.newArrayList(0));
    context.put(OPENSHIFT_STORAGETYPE_LIST, getStorageTypes());
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
    Arrays.asList(
            OPENSHIFT_SERVER_ADDRESS,
            OPENSHIFT_USERNAME,
            OPENSHIFT_PW,
            OPENSHIFT_NAMESPACE,
            OPENSHIFT_DEPLOYMENTCONFIG_NAME,
            OPENSHIFT_DEPLOYMENT_REPLICAS,
            OPENSHIFT_DEPLOYMENT_STRATEGY)
        .forEach(field -> context.put(field, taskDefinition.getConfiguration().get(field)));
    context.put(OPENSHIFT_DEPLOYMENT_STRATEGIES, getDeploymentStrategies());
    context.put(OPENSHIFT_STORAGETYPE_LIST, getStorageTypes());
    ArrayList<String> containers = Lists.newArrayList();
    ArrayList<String> containerStorages = Lists.newArrayList();
    buildConfigElements = Utils.getBuildConfigElements(context);
    context.put(OPENSHIFT_BUILDCONFIGS_LIST, buildConfigElements);
    Set<String> keys = taskDefinition.getConfiguration().keySet();
    keys.parallelStream()
        .filter(key -> key.startsWith(OPENSHIFT_CONTAINER_IMAGE))
        .forEach(
            image -> {
              containers.add(StringUtils.remove(image, OPENSHIFT_CONTAINER_IMAGE));
              context.put(image, taskDefinition.getConfiguration().get(image));
            });
    keys.parallelStream()
        .filter(key -> key.startsWith(OPENSHIFT_CONTAINER_STORAGE_NAME))
        .forEach(
            storage -> {
              containerStorages.add(StringUtils.remove(storage, OPENSHIFT_CONTAINER_STORAGE_NAME));
              context.put(storage, taskDefinition.getConfiguration().get(storage));
            });
    keys.parallelStream()
        .filter(
            key ->
                Arrays.asList(
                        OPENSHIFT_CONTAINER_PORTS,
                        OPENSHIFT_CONTAINER_NAME,
                        OPENSHIFT_CONTAINER_VARS,
                        OPENSHIFT_CONTAINER_STORAGE_TYPE,
                        OPENSHIFT_CONTAINER_STORAGE_MOUNT,
                        OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT,
                        OPENSHIFT_CONTAINER_STORAGE_PVC_NAME)
                    .parallelStream()
                    .anyMatch(key::startsWith))
        .forEach(con -> context.put(con, taskDefinition.getConfiguration().get(con)));
    context.put("containerList", containers);
    context.put("storageList", containerStorages);
    context.put(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS_STRATEGIES, getDeploymentConfigExistsMap());
    context.put(
        OPENSHIFT_DEPLOYMENTCONFIG_EXISTS,
        taskDefinition.getConfiguration().get(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS));
  }

  @Override
  public void validate(
      @NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
    if (StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_TOKEN))
        || StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_SERVER))) {
      Arrays.asList(OPENSHIFT_SERVER_ADDRESS, OPENSHIFT_USERNAME, OPENSHIFT_PW)
          .parallelStream()
          .filter(field -> StringUtils.isEmpty(params.getString(field, EMPTY_STRING)))
          .collect(Collectors.toList())
          .forEach(f -> errorCollection.addError(f, FIELD_EMPTY_ERROR));
    }

    params
        .keySet()
        .parallelStream()
        .filter(key -> StringUtils.isEmpty(params.getString(key, EMPTY_STRING)))
        .filter(
            key ->
                (key.startsWith(OPENSHIFT_CONTAINER_STORAGE_NAME) && !key.contains(Constants.PVC))
                    || (key.startsWith(OPENSHIFT_CONTAINER_STORAGE_PVC_NAME)
                        && StringUtils.equals(
                            params.getString(
                                OPENSHIFT_CONTAINER_STORAGE_TYPE
                                    + StringUtils.strip(key, OPENSHIFT_CONTAINER_STORAGE_PVC_NAME)),
                            Constants.PVC)))
        .collect(Collectors.toList())
        .forEach(emptyField -> errorCollection.addError(emptyField, FIELD_EMPTY_ERROR));

    HashMap<String, Pattern> regexPatterns = new HashMap<>();
    regexPatterns.put(OPENSHIFT_DEPLOYMENTCONFIG_NAME, DEPLOYMENTCONFIG_NAME_PATTERN);
    regexPatterns.put(OPENSHIFT_NAMESPACE, Constants.OPENSHIFT_NAMESPACE_CONTAINER_PATTERN);

    HashMap<String, String> patternErrors = new HashMap<>();
    patternErrors.put(OPENSHIFT_DEPLOYMENTCONFIG_NAME, INVALID_DEPLOYMENTCONFIG_NAME);
    patternErrors.put(OPENSHIFT_NAMESPACE, INVALID_NAMESPACE_NAME);

    regexPatterns
        .keySet()
        .parallelStream()
        .filter(k -> StringUtils.isEmpty(params.getString(k, EMPTY_STRING)))
        .collect(Collectors.toList())
        .forEach(k -> errorCollection.addError(k, FIELD_EMPTY_ERROR));
    regexPatterns
        .entrySet()
        .parallelStream()
        .filter(map -> !StringUtils.isEmpty(params.getString(map.getKey(), EMPTY_STRING)))
        .filter(
            map -> !map.getValue().matcher(params.getString(map.getKey(), EMPTY_STRING)).matches())
        .collect(Collectors.toList())
        .forEach(map -> errorCollection.addError(map.getKey(), patternErrors.get(map.getKey())));

    params
        .keySet()
        .parallelStream()
        .filter(k -> k.startsWith(OPENSHIFT_CONTAINER_PORTS))
        .collect(Collectors.toList())
        .forEach(
            portString -> {
              String strippedPortString =
                  StringUtils.replace(params.getString(portString), SPACE, EMPTY_STRING);
              if (!StringUtils.isEmpty(strippedPortString)
                  && !PORTS_PATTERN.matcher(strippedPortString).matches()) {
                errorCollection.addError(portString, Constants.INVALID_PORT_DEFINITION);
              }
            });

    List<String> containerNames =
        params
            .keySet()
            .parallelStream()
            .filter(k -> k.startsWith(OPENSHIFT_CONTAINER_NAME))
            .collect(Collectors.toList());

    containerNames.forEach(
        containerName -> {
          if (params.getString(containerName, EMPTY_STRING).isEmpty()) {
            errorCollection.addError(containerName, FIELD_EMPTY_ERROR);
          } else if (!OPENSHIFT_NAMESPACE_CONTAINER_PATTERN
              .matcher(params.getString(containerName, EMPTY_STRING))
              .matches()) {
            errorCollection.addError(containerName, INVALID_CONTAINER_NAME);
          }
        });

    List<String> containerNamesNames =
        containerNames
            .parallelStream()
            .map(names -> params.getString(names, EMPTY_STRING))
            .filter(names -> !names.isEmpty())
            .collect(Collectors.toList());

    params
        .keySet()
        .parallelStream()
        .filter(k -> k.startsWith(OPENSHIFT_CONTAINER_STORAGE_MOUNT))
        .filter(
            containerStorageMount ->
                !containerNamesNames.contains(
                    params.getString(containerStorageMount, EMPTY_STRING)))
        .collect(Collectors.toList())
        .forEach(
            containerStorageMount ->
                errorCollection.addError(
                    containerStorageMount, Constants.THIS_CONTAINER_DOES_NOT_EXIST));

    super.validate(params, errorCollection);
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
            OPENSHIFT_NAMESPACE,
            OPENSHIFT_DEPLOYMENT_REPLICAS,
            OPENSHIFT_DEPLOYMENT_STRATEGY,
            OPENSHIFT_DEPLOYMENTCONFIG_NAME)
        .forEach(field -> config.put(field, params.getString(field)));
    Set<String> keys = params.keySet();
    keys.parallelStream()
        .filter(key -> key.startsWith(OPENSHIFT_CONTAINER_IMAGE))
        .forEach(
            imag -> {
              config.put(imag, params.getString(imag));
              buildConfigElements
                  .parallelStream()
                  .forEach(
                      element -> {
                        String id = params.getString(imag);
                        if (id != null && element.getId() == Integer.parseInt(id)) {
                          config.put(Constants.NAME_U + imag, element.getBuildConfigOutputName());
                        }
                      });
            });
    keys.parallelStream()
        .filter(
            key ->
                Arrays.asList(
                        OPENSHIFT_CONTAINER_PORTS,
                        OPENSHIFT_CONTAINER_NAME,
                        OPENSHIFT_CONTAINER_VARS,
                        OPENSHIFT_CONTAINER_STORAGE_TYPE,
                        OPENSHIFT_CONTAINER_STORAGE_MOUNT,
                        OPENSHIFT_CONTAINER_STORAGE_NAME,
                        OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT,
                        OPENSHIFT_CONTAINER_STORAGE_PVC_NAME)
                    .parallelStream()
                    .anyMatch(key::startsWith))
        .forEach(con -> config.put(con, params.getString(con)));
    config.put(
        OPENSHIFT_DEPLOYMENTCONFIG_EXISTS, params.getString(OPENSHIFT_DEPLOYMENTCONFIG_EXISTS));
    return config;
  }

  /**
   * Creates a Map with key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with options for handling existing deployment
   */
  private Map<String, String> getDeploymentConfigExistsMap() {
    Map<String, String> deploymentConfigExistsMap = new LinkedHashMap<>();
    deploymentConfigExistsMap.put(Constants.OVERWRITE, Constants.OVERWRITE_IT);
    deploymentConfigExistsMap.put(Constants.IGNORE, Constants.IGNORE_NEW_SETTINGS);
    deploymentConfigExistsMap.put(Constants.FAIL, Constants.FAIL_THE_BUILD_PROCESS);
    return deploymentConfigExistsMap;
  }

  /**
   * Creates a Map with key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with options for deployment strategies
   */
  private Map<String, String> getDeploymentStrategies() {
    Map<String, String> deploymentStrategyMap = new LinkedHashMap<>();
    deploymentStrategyMap.put(Constants.OCP_DEPLOYMENT_STRATEGY_ROLLING, Constants.ROLLING);
    deploymentStrategyMap.put(Constants.OCP_DEPLOYMENT_STRATEGY_RECREATE, Constants.RECREATE);
    return deploymentStrategyMap;
  }

  /**
   * Creates a Map with key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with options for supported storage types
   */
  private Map<String, String> getStorageTypes() {
    Map<String, String> storageTypeMap = new LinkedHashMap<>();
    storageTypeMap.put(Constants.EMPTYDIR, Constants.EMPTY_DIR);
    storageTypeMap.put(Constants.PVC, Constants.PERSISTENT_VOLUME_CLAIM);
    return storageTypeMap;
  }
}
