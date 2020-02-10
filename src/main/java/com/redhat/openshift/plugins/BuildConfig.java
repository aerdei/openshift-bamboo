package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILDCONFIG_NAME;

import com.atlassian.bamboo.task.TaskContext;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Instances of this class represent a buildConfig object created from a template and then modified
 * to requirements.
 *
 * @author Attila Erdei
 */
class BuildConfig {
  private JSONObject buildConfigObject;
  private TaskContext taskContext;

  /**
   * Creates a BuildConfig object.
   *
   * @param taskContext TaskContext for the current plugin task. Used for retrieving task configs
   */
  BuildConfig(TaskContext taskContext) {
    JSONObject buildConfigObjectTempl =
        new JSONObject(Templates.getInstance(taskContext).getBuildConfigTemplate());
    buildConfigObjectTempl
        .getJSONObject(Constants.METADATA)
        .put(Constants.NAME, taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME));
    buildConfigObjectTempl
        .getJSONObject(Constants.METADATA)
        .getJSONObject(Constants.LABELS)
        .put(Constants.BUILD, taskContext.getConfigurationMap().get(OPENSHIFT_BUILDCONFIG_NAME));
    this.buildConfigObject = buildConfigObjectTempl;
    this.taskContext = taskContext;
  }

  /**
   * Sets the build source to binary. Build input has to be provided in a tar through a byte stream.
   */
  void setBinarySource() {
    JSONObject buildSourceTemplate =
        new JSONObject(Templates.getInstance(this.taskContext).getBuildSourceTemplate());
    buildSourceTemplate.getJSONObject(Constants.SOURCE).put(Constants.TYPE, Constants.BINARY_CAP);
    buildSourceTemplate.put(Constants.GIT, JSONObject.NULL);
    this.buildConfigObject.put(Constants.SPEC, buildSourceTemplate);
  }

  /**
   * Sets the build input to a Git repo. Sources will be pulled from this repo at build time.
   *
   * @param gitAddressName URL of the Git repo to be used
   */
  void setGitSource(String gitAddressName) {
    JSONObject buildSourceTemplate =
        new JSONObject(Templates.getInstance(this.taskContext).getBuildSourceTemplate());
    buildSourceTemplate.put(Constants.TYPE, Constants.GIT_CAP);
    buildSourceTemplate.getJSONObject(Constants.GIT).put(Constants.URI, gitAddressName);
    buildSourceTemplate.put(Constants.BINARY, JSONObject.NULL);
    this.buildConfigObject.put(
        Constants.SPEC,
        buildConfigObject.getJSONObject(Constants.SPEC).put(Constants.SOURCE, buildSourceTemplate));
  }

  /**
   * Sets the build strategy type to source and sets the builder image and build environment
   * variables.
   *
   * @param builderImage name or URL of the image to be used as a builder
   * @param builderImageType type of builder image to be used
   * @param envVars build environment variables
   */
  void setSourceStrategy(String builderImage, String builderImageType, String envVars) {
    JSONObject buildStrategyTemplateJson =
        new JSONObject(Templates.getInstance(this.taskContext).getBuildStrategyTemplate());
    buildStrategyTemplateJson.put(Constants.TYPE, Constants.SOURCE_CAP);
    buildStrategyTemplateJson.put(Constants.DOCKER_STRATEGY, JSONObject.NULL);
    buildStrategyTemplateJson
        .getJSONObject(Constants.SOURCE_STRATEGY)
        .put(Constants.FROM, setBuilderImage(builderImage, builderImageType));
    buildStrategyTemplateJson
        .getJSONObject(Constants.SOURCE_STRATEGY)
        .put(Constants.ENV, compileBuildEnvVars(envVars));
    this.buildConfigObject.put(
        Constants.SPEC,
        buildConfigObject
            .getJSONObject(Constants.SPEC)
            .put(Constants.STRATEGY, buildStrategyTemplateJson));
  }

  /**
   * Compiles the builder image section. Processes imageStreamTags by splitting the input text at
   * the first slash. First part is the namespace, second part is the image name and tag. If there
   * is no slash, the entire input will be treated as an image. If the image type is dockerImage,
   * the text will be treated as image name.
   *
   * @param builderImage name of the builder image and optionally the namespace
   * @param builderImageType
   * @return
   */
  private JSONObject setBuilderImage(String builderImage, String builderImageType) {
    JSONObject builderImageFrom = new JSONObject();
    builderImageFrom.put(Constants.KIND, builderImageType);
    if (builderImageType.equals(Constants.IMAGESTREAMTAG)) {
      if (!builderImage.isEmpty()) {
        String[] splitBuilderImage = StringUtils.splitByWholeSeparator(builderImage, "/", 2);
        builderImageFrom.put(Constants.NAME, splitBuilderImage[splitBuilderImage.length - 1]);
        if (splitBuilderImage.length > 1) {
          builderImageFrom.put(Constants.NAMESPACE, splitBuilderImage[0]);
        }
      }
    } else if (builderImageType.equals(Constants.DOCKERIMAGE)) {
      builderImageFrom.put(Constants.NAME, builderImage);
    }
    return builderImageFrom;
  }

  /**
   * Sets the strategy to dockerStrategy, populates the build environment variables, and sets up the
   * builder image and type.
   *
   * @param builderImage name of the builder image
   * @param builderImageType type of the builder image
   * @param envVars comma-separated build environment variables
   */
  void setDockerStrategy(String builderImage, String builderImageType, String envVars) {
    JSONObject buildStrategyTemplateJson =
        new JSONObject(Templates.getInstance(this.taskContext).getBuildStrategyTemplate());
    buildStrategyTemplateJson.put(Constants.SOURCE_STRATEGY, JSONObject.NULL);
    buildStrategyTemplateJson.put(Constants.TYPE, Constants.DOCKER);
    buildStrategyTemplateJson
        .getJSONObject(Constants.DOCKER_STRATEGY)
        .put(Constants.ENV, compileBuildEnvVars(envVars));
    if (!builderImage.isEmpty()) {
      buildStrategyTemplateJson
          .getJSONObject(Constants.DOCKER_STRATEGY)
          .put(Constants.FROM, setBuilderImage(builderImage, builderImageType));
    } else {
      buildStrategyTemplateJson
          .getJSONObject(Constants.DOCKER_STRATEGY)
          .put(Constants.FROM, JSONObject.NULL);
    }
    this.buildConfigObject.put(
        Constants.SPEC,
        buildConfigObject
            .getJSONObject(Constants.SPEC)
            .put(Constants.STRATEGY, buildStrategyTemplateJson));
  }

  /**
   * Sets the output to an imagestream.
   *
   * @param imageStream name of the imagestream to be used as output
   */
  void setImageOutputImageStream(String imageStream) {
    this.buildConfigObject
        .getJSONObject(Constants.SPEC)
        .put(
            Constants.OUTPUT,
            new JSONObject()
                .put(
                    Constants.TO,
                    new JSONObject()
                        .put(Constants.KIND, Constants.IMAGESTREAMTAG)
                        .put(Constants.NAME, imageStream)));
  }

  /**
   * Sets the output as a Docker image.
   *
   * @param dockerImage name (path) of the Docker image to be used as output
   */
  void setImageOutputDockerImage(String dockerImage) {
    this.buildConfigObject
        .getJSONObject(Constants.SPEC)
        .put(
            Constants.OUTPUT,
            new JSONObject()
                .put(
                    Constants.TO,
                    new JSONObject()
                        .put(Constants.KIND, Constants.DOCKERIMAGE)
                        .put(Constants.NAME, dockerImage)));
  }

  /**
   * Sets the source secret in a buildConfig
   *
   * @param sourceSecretName name of the source secret to be used
   */
  void setSourceSecret(String sourceSecretName) {
    if (!sourceSecretName.isEmpty()) {
      this.buildConfigObject
          .getJSONObject(Constants.SPEC)
          .getJSONObject(Constants.SOURCE)
          .put(Constants.SOURCE_SECRET, new JSONObject().put(Constants.NAME, sourceSecretName));
    }
  }

  /**
   * Compiles the build environment variables from a string into a JSON Array. Variables are split
   * at commas. Keys and values are split at equals signs.
   *
   * @param envVariables the comma-separated environment variables as a string
   * @return a JSON Array containing name-value pairs of the variables
   */
  private JSONArray compileBuildEnvVars(String envVariables) {
    JSONArray vars = new JSONArray();
    for (String var :
        org.apache.commons.lang3.StringUtils.split(
            org.apache.commons.lang3.StringUtils.replace(
                envVariables, Constants.SPACE, Constants.EMPTY_STRING),
            Constants.COMMA)) {
      JSONObject varsConfigJsonObject =
          new JSONObject(Templates.getInstance(taskContext).getEnvVarsTemplate());
      String[] varsParts = org.apache.commons.lang3.StringUtils.split(var, Constants.EQUALS);
      varsConfigJsonObject.put(Constants.NAME, varsParts[0]);
      varsConfigJsonObject.put(Constants.VALUE, varsParts[1]);
      vars.put(new JSONObject(JSONObject.valueToString(varsConfigJsonObject)));
    }
    return vars;
  }

  /** @return the buildConfig JsonObject */
  JSONObject getJsonObject() {
    return this.buildConfigObject;
  }
}
