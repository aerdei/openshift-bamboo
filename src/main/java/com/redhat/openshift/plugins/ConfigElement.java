package com.redhat.openshift.plugins;

/**
 * This class is used for storing buildConfigs or deploymentConfigs in case there are multiple ones
 * in a build plan. It is used when polling for configs read from context (usually drop-downs) where
 * the selection is represented by a number.
 *
 * @author Attila Erdei
 */
class ConfigElement {

  @SuppressWarnings("WeakerAccess") // .ftl needs it as public
  public final long id;

  @SuppressWarnings({"WeakerAccess", "unused"}) // .ftl needs it as public, and it uses it
  public final String taskUserDescription;

  @SuppressWarnings("WeakerAccess") // .ftl needs it as public
  public final String configName;

  @SuppressWarnings("WeakerAccess") // .ftl needs it as public
  public final String configNamespace;

  @SuppressWarnings("WeakerAccess") // .ftl needs it as public
  public final String buildConfigOutputName;

  private ConfigElement(
      long id,
      String taskUserDescription,
      String configName,
      String configNamespace,
      String buildConfigOutputName) {
    this.id = id;
    this.taskUserDescription = taskUserDescription;
    this.configName = configName;
    this.configNamespace = configNamespace;
    this.buildConfigOutputName = buildConfigOutputName;
  }

  static ConfigElement createBuildConfigElement(
      long id,
      String taskUserDescription,
      String configName,
      String configNamespace,
      String buildConfigOutputName) {
    return new ConfigElement(
        id, taskUserDescription, configName, configNamespace, buildConfigOutputName);
  }

  static ConfigElement createDeploymentConfigElement(
      long id, String taskUserDescription, String configName, String configNamespace) {
    return new ConfigElement(id, taskUserDescription, configName, configNamespace, null);
  }

  long getId() {
    return id;
  }

  /**
   * Returns the name of the config.
   *
   * @return name of config
   */
  String getConfigName() {
    return configName;
  }

  /**
   * Returns the namespace of config.
   *
   * @return namespace of config
   */
  String getConfigNamespace() {
    return configNamespace;
  }

  /**
   * Returns the output name of config.
   *
   * @return output name of config
   */
  String getBuildConfigOutputName() {
    return buildConfigOutputName;
  }
}
