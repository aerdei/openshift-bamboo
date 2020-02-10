package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.ARTIFACT_LOCATION;
import static com.redhat.openshift.plugins.Constants.ARTIFACT_LOCATIONS;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_SERVER;
import static com.redhat.openshift.plugins.Constants.BAMBOO_VAR_OCP_TOKEN;
import static com.redhat.openshift.plugins.Constants.DEFAULT_RUBY_EX_LATEST;
import static com.redhat.openshift.plugins.Constants.EMPTY_STRING;
import static com.redhat.openshift.plugins.Constants.FIELD_EMPTY_ERROR;
import static com.redhat.openshift.plugins.Constants.INVALID_BUILDCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.INVALID_NAMESPACE_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_EXISTS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_EXISTS_STRATEGIES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDER_IMAGE_TYPE_GIT;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_ENV_VARS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_STRATEGIES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_STRATEGY;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_DOCKER_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_IMAGESTREAM_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_TYPE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_OUTPUT_TYPES;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_PW;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_REPOSITORY_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_USERNAME;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_ARTIFACT_ARTIFACTORY;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_DOCKERFILE_GIT;
import static com.redhat.openshift.plugins.Constants.REPOSITORY_ADDRESS_SOURCE_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_ARTIFACT_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_DOCKERFILE_GIT;
import static com.redhat.openshift.plugins.Constants.SOURCE_SECRET_SOURCE_GIT;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.VariableDefinitionManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class is used to configure parameters for BuildConfigTask. It sets up default values,
 * populates drop-downs and textboxes, and upon saving, puts them into the task configuration map to
 * be used by other classes.
 *
 * @author Attila Erdei
 */
public class BuildConfigTaskConfigurator extends AbstractTaskConfigurator {
  @ComponentImport private final VariableDefinitionManager variableDefinitionManager;
  @ComponentImport private final PlanManager planManager;

  public BuildConfigTaskConfigurator(
      @NotNull final VariableDefinitionManager variableDefinitionManager,
      @NotNull final PlanManager planManager) {
    this.variableDefinitionManager = variableDefinitionManager;
    this.planManager = planManager;
  }

  /**
   * Populates task config map by putting the key-value pairs where key is the Bamboo task UI
   * element name, value is the input.
   *
   * @param params parameters from the user interface
   * @param previousTaskDefinition intance of previous taskDefinition
   * @return a configuration Map with string key-value pairs
   */
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
            OPENSHIFT_BUILDCONFIG_NAME,
            OPENSHIFT_BUILD_STRATEGY,
            OPENSHIFT_REPOSITORY_ADDRESS,
            OPENSHIFT_BUILDER_IMAGE_GIT,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO,
            OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE,
            OPENSHIFT_BUILDER_IMAGE_TYPE_GIT,
            OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT,
            OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY,
            OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO,
            OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE,
            ARTIFACT_LOCATION,
            REPOSITORY_ADDRESS_ARTIFACT_GIT,
            REPOSITORY_ADDRESS_ARTIFACT_ARTIFACTORY,
            REPOSITORY_ADDRESS_DOCKERFILE_GIT,
            REPOSITORY_ADDRESS_SOURCE_GIT,
            SOURCE_SECRET_SOURCE_GIT,
            SOURCE_SECRET_ARTIFACT_GIT,
            SOURCE_SECRET_DOCKERFILE_GIT,
            OPENSHIFT_BUILD_ENV_VARS,
            OPENSHIFT_OUTPUT_TYPE,
            OPENSHIFT_OUTPUT_IMAGESTREAM_NAME,
            OPENSHIFT_OUTPUT_DOCKER_NAME,
            OPENSHIFT_BUILDCONFIG_EXISTS)
        .forEach(field -> config.put(field, params.getString(field)));
    return config;
  }

  /**
   * Upon saving, validates the Bamboo UI element input fields by comparing them to conditions.
   *
   * @param params parameters from the user interface
   * @param errorCollection errorcollection instance to add error messages to the UI
   */
  @Override
  public void validate(
      @NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
    super.validate(params, errorCollection);
    if (StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_TOKEN))
        || StringUtils.isEmpty(params.getString(BAMBOO_VAR_OCP_SERVER))) {
      Arrays.asList(Constants.OPENSHIFT_SERVER_ADDRESS, OPENSHIFT_USERNAME, OPENSHIFT_PW)
          .forEach(
              field -> {
                if (StringUtils.isEmpty(params.getString(field))) {
                  errorCollection.addError(field, FIELD_EMPTY_ERROR);
                }
              });
    }

    HashMap<String, String> buildStrategies = new HashMap<>();
    buildStrategies.put(Constants.SOURCE, Constants.SOURCE);
    buildStrategies.put(Constants.ARTIFACT, params.getString(ARTIFACT_LOCATION));
    buildStrategies.put(Constants.DOCKERFILE, Constants.DOCKERFILE);

    HashMap<String, List<String>> sourceBuildStrategyFields = new HashMap<>();
    sourceBuildStrategyFields.put(
        Constants.SOURCE,
        Arrays.asList(Constants.REPOSITORY_ADDRESS_SOURCE_GIT, OPENSHIFT_BUILDER_IMAGE_GIT));

    HashMap<String, List<String>> artifactBuildStrategyFields = new HashMap<>();
    artifactBuildStrategyFields.put(
        Constants.GIT,
        Arrays.asList(
            Constants.REPOSITORY_ADDRESS_ARTIFACT_GIT,
            Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT));
    artifactBuildStrategyFields.put(
        Constants.ARTIFACTORY,
        Arrays.asList(
            Constants.REPOSITORY_ADDRESS_ARTIFACT_ARTIFACTORY,
            Constants.OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY));
    artifactBuildStrategyFields.put(
        Constants.BAMBOO_ARTIFACT,
        Collections.singletonList(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO));

    HashMap<String, List<String>> dockerFileBuildStrategyFields = new HashMap<>();
    dockerFileBuildStrategyFields.put(
        Constants.DOCKERFILE,
        Collections.singletonList(Constants.REPOSITORY_ADDRESS_DOCKERFILE_GIT));

    HashMap<String, HashMap<String, List<String>>> buildStrategyFields = new HashMap<>();
    buildStrategyFields.put(Constants.SOURCE, sourceBuildStrategyFields);
    buildStrategyFields.put(Constants.ARTIFACT, artifactBuildStrategyFields);
    buildStrategyFields.put(Constants.DOCKERFILE, dockerFileBuildStrategyFields);

    buildStrategyFields
        .get(params.getString(OPENSHIFT_BUILD_STRATEGY))
        .get(buildStrategies.get(params.getString(OPENSHIFT_BUILD_STRATEGY)))
        .forEach(
            val -> {
              if (StringUtils.isEmpty(params.getString(val))) {
                errorCollection.addError(val, FIELD_EMPTY_ERROR);
              }
            });

    HashMap<String, String> outputTypeFields = new HashMap<>();
    outputTypeFields.put(Constants.IMAGESTREAM, OPENSHIFT_OUTPUT_IMAGESTREAM_NAME);
    outputTypeFields.put(Constants.DOCKER_REPO, OPENSHIFT_OUTPUT_DOCKER_NAME);

    if (StringUtils.isEmpty(outputTypeFields.get(params.getString(OPENSHIFT_OUTPUT_TYPE)))) {
      errorCollection.addError(
          outputTypeFields.get(params.getString(OPENSHIFT_OUTPUT_TYPE)), FIELD_EMPTY_ERROR);
    }

    HashMap<String, Pattern> regexPatterns = new HashMap<>();
    regexPatterns.put(OPENSHIFT_BUILDCONFIG_NAME, Constants.BUILDCONFIG_NAME_PATTERN);
    regexPatterns.put(OPENSHIFT_NAMESPACE, Constants.OPENSHIFT_NAMESPACE_CONTAINER_PATTERN);

    HashMap<String, String> patternErrors = new HashMap<>();
    patternErrors.put(OPENSHIFT_BUILDCONFIG_NAME, INVALID_BUILDCONFIG_NAME);
    patternErrors.put(OPENSHIFT_NAMESPACE, INVALID_NAMESPACE_NAME);

    regexPatterns.forEach(
        (k, v) -> {
          if (StringUtils.isEmpty(params.getString(k))) {
            errorCollection.addError(k, FIELD_EMPTY_ERROR);
          } else if (!v.matcher(params.getString(k, EMPTY_STRING)).matches()) {
            errorCollection.addError(k, patternErrors.get(k));
          }
        });
  }

  /**
   * Populates the Bamboo UI elements with defaults or drop-down selections.
   *
   * @param context context used to configure user interface elements
   */
  @Override
  public void populateContextForCreate(@NotNull final Map<String, Object> context) {
    super.populateContextForCreate(context);
    Utils.setupBambooVariables(
        Arrays.asList(BAMBOO_VAR_OCP_TOKEN, BAMBOO_VAR_OCP_SERVER),
        context,
        variableDefinitionManager,
        planManager);
    context.put(OPENSHIFT_SERVER_ADDRESS, Constants.DEFAULT_HTTPS_192_168_42_237_8443);
    context.put(OPENSHIFT_USERNAME, Constants.DEFAULT_ADMIN);
    context.put(OPENSHIFT_NAMESPACE, Constants.DEFAULT_BAMBOO_NAMESPACE);
    context.put(OPENSHIFT_BUILDCONFIG_NAME, Constants.DEFAULT_RUBY_EX);
    context.put(OPENSHIFT_BUILD_STRATEGY, Constants.SOURCE);
    context.put(OPENSHIFT_BUILD_STRATEGIES, getBuildStrategyMap());
    context.put(ARTIFACT_LOCATION, Constants.GIT);
    context.put(ARTIFACT_LOCATIONS, getArtifactSourceMap());
    context.put(OPENSHIFT_BUILDER_IMAGE_GIT, Constants.DEFAULT_RUBY_22_CENTOS_7_LATEST);
    context.put(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT, Constants.DEFAULT_RUBY_22_CENTOS_7_LATEST);
    context.put(
        OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY, Constants.DEFAULT_RUBY_22_CENTOS_7_LATEST);
    context.put(OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO, Constants.DEFAULT_RUBY_22_CENTOS_7_LATEST);
    context.put(OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE, Constants.DEFAULT_RUBY_22_CENTOS_7_LATEST);
    context.put(OPENSHIFT_BUILDCONFIG_EXISTS, Constants.OVERWRITE);
    context.put(OPENSHIFT_BUILDCONFIG_EXISTS_STRATEGIES, getBuildConfigExistsMap());
    context.put(OPENSHIFT_OUTPUT_TYPES, getOutputTypesMap());
    context.put(OPENSHIFT_OUTPUT_TYPE, Constants.IMAGESTREAM);
    context.put(OPENSHIFT_OUTPUT_IMAGESTREAM_NAME, DEFAULT_RUBY_EX_LATEST);
    context.put(OPENSHIFT_BUILDER_IMAGE_TYPES, getBuilderImageTypeMap());
  }

  /**
   * Populates the Bamboo UI interface with saved configuration values when it is reopened for
   * editing. Re-populates drop-down selections.
   *
   * @param context context used to configure user interface elements
   * @param taskDefinition taskdefinition instance to fetch configuration map from
   */
  @Override
  public void populateContextForEdit(
      @NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
    super.populateContextForEdit(context, taskDefinition);
    Utils.setupBambooVariables(
        Arrays.asList(BAMBOO_VAR_OCP_TOKEN, BAMBOO_VAR_OCP_SERVER),
        context,
        variableDefinitionManager,
        planManager);

    context.put(OPENSHIFT_BUILD_STRATEGIES, getBuildStrategyMap());
    context.put(ARTIFACT_LOCATIONS, getArtifactSourceMap());
    context.put(OPENSHIFT_OUTPUT_TYPES, getOutputTypesMap());
    context.put(OPENSHIFT_BUILDCONFIG_EXISTS_STRATEGIES, getBuildConfigExistsMap());
    context.put(OPENSHIFT_BUILDER_IMAGE_TYPES, getBuilderImageTypeMap());

    Arrays.asList(
            OPENSHIFT_SERVER_ADDRESS,
            OPENSHIFT_USERNAME,
            OPENSHIFT_PW,
            OPENSHIFT_NAMESPACE,
            OPENSHIFT_BUILD_STRATEGY,
            ARTIFACT_LOCATION,
            OPENSHIFT_BUILDER_IMAGE_GIT,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY,
            OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO,
            OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE,
            OPENSHIFT_BUILDER_IMAGE_TYPE_GIT,
            OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT,
            OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY,
            OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO,
            OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE,
            REPOSITORY_ADDRESS_ARTIFACT_GIT,
            REPOSITORY_ADDRESS_ARTIFACT_ARTIFACTORY,
            REPOSITORY_ADDRESS_DOCKERFILE_GIT,
            REPOSITORY_ADDRESS_SOURCE_GIT,
            SOURCE_SECRET_SOURCE_GIT,
            SOURCE_SECRET_ARTIFACT_GIT,
            SOURCE_SECRET_DOCKERFILE_GIT,
            OPENSHIFT_BUILDCONFIG_EXISTS,
            OPENSHIFT_BUILDCONFIG_NAME,
            OPENSHIFT_BUILD_ENV_VARS,
            OPENSHIFT_OUTPUT_TYPE,
            OPENSHIFT_OUTPUT_IMAGESTREAM_NAME,
            OPENSHIFT_OUTPUT_DOCKER_NAME)
        .forEach(field -> context.put(field, taskDefinition.getConfiguration().get(field)));
  }

  /**
   * Creates a Map with build strategy key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with build strategies
   */
  private Map<String, String> getBuildStrategyMap() {
    Map<String, String> buildStrategyMap = new LinkedHashMap<>();
    buildStrategyMap.put(Constants.SOURCE, Constants.DIRECTLY_FROM_SOURCE_CODE);
    buildStrategyMap.put(Constants.DOCKERFILE, Constants.USING_A_DOCKERFILE);
    return buildStrategyMap;
  }

  /**
   * Creates a Map with builder image type key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with builder image types
   */
  private Map<String, String> getBuilderImageTypeMap() {
    Map<String, String> builderImageTypeMap = new LinkedHashMap<>();
    builderImageTypeMap.put(Constants.IMAGESTREAMTAG, Constants.OPEN_SHIFT_IMAGE_STREAM_TAG);
    builderImageTypeMap.put(Constants.DOCKERIMAGE, Constants.DOCKER_IMAGE);
    return builderImageTypeMap;
  }

  /**
   * Creates a Map with possible artifact source key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with possible artifact sources
   */
  private Map<String, String> getArtifactSourceMap() {
    Map<String, String> artifactSourceMap = new LinkedHashMap<>();
    artifactSourceMap.put(Constants.BAMBOO_ARTIFACT, Constants.A_BAMBOO_ARTIFACT);
    artifactSourceMap.put(Constants.GIT, Constants.IN_A_GIT_REPOSITORY);
    artifactSourceMap.put(Constants.ARTIFACTORY, Constants.IN_ARTIFACTORY);
    return artifactSourceMap;
  }

  /**
   * Creates a Map with key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with options for handling existing build configs
   */
  private Map<String, String> getBuildConfigExistsMap() {
    Map<String, String> buildConfigExistsMap = new LinkedHashMap<>();
    buildConfigExistsMap.put(Constants.OVERWRITE, Constants.OVERWRITE_IT);
    buildConfigExistsMap.put(Constants.IGNORE, Constants.IGNORE_NEW_SETTINGS);
    buildConfigExistsMap.put(Constants.FAIL, Constants.FAIL_THE_BUILD_PROCESS);
    return buildConfigExistsMap;
  }

  /**
   * Creates a Map with output type key-value pairs used in drop-downs.
   *
   * @return Map of String key-value pairs with output types
   */
  private Map<String, String> getOutputTypesMap() {
    Map<String, String> outputTypesMap = new LinkedHashMap<>();
    outputTypesMap.put(Constants.IMAGESTREAM, Constants.AN_IMAGESTREAM);
    outputTypesMap.put(Constants.DOCKER_REPO, Constants.AN_EXTERNAL_DOCKER_IMAGE);
    return outputTypesMap;
  }
}
