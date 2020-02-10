package com.redhat.openshift.plugins;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Contains constants used by the OpenShift plugin in Bamboo.
 *
 * @author Attila Erdei
 */
final class Constants {

  static final String ACCEPT = "accept";
  static final String AN_ERROR_OCCURED = "An error occured: ";
  static final String AN_EXTERNAL_DOCKER_IMAGE = "...an external docker image";
  static final String AN_IMAGESTREAM = "...an imagestream";
  static final String API_V_1_NAMESPACES = "/api/v1/namespaces/";
  static final String APP = "app";
  static final String APPLICATION_JSON = "application/json";
  static final String APPLICATION_JSON_WILDCARD = "application/json, */*";
  static final String APPLICATION_MERGE_PATCH_JSON = "application/merge-patch+json";
  static final String APPLICATION_STRATEGIC_MERGE_PATCH_JSON =
      "application/strategic-merge-patch+json";
  static final String ARTIFACT = "artifact";
  static final String ARTIFACTORY = "artifactory";
  static final String ARTIFACTORY_SERVER_TOKEN = "ARTIFACTORY_TOKEN";
  static final String ARTIFACTORY_SERVER_URL = "ARTIFACTORY_URL";
  static final String ARTIFACT_LOCATION = "artifactlocation";
  static final String ARTIFACT_LOCATIONS = "artifactlocations";
  static final String AUTHORIZATION = "authorization";
  static final String A_BAMBOO_ARTIFACT = "...a Bamboo artifact";
  static final String BAMBOO_ARTIFACT = "bamboo_artifact";
  static final String BAMBOO_BC_EXISTS_FAIL = "fail";
  static final String BAMBOO_BC_EXISTS_IGNORE = "ignore";
  static final String BAMBOO_BC_EXISTS_OVERWRITE = "overwrite";
  static final String BAMBOO_DC_EXISTS_IGNORE = "ignore";
  static final String BAMBOO_DC_EXISTS_OVERWRITE = "overwrite";
  static final String BAMBOO_VAR_OCP_SERVER = "OCP_SERVER";
  static final String BAMBOO_VAR_OCP_TOKEN = "OCP_TOKEN";
  static final String BC_PLUGIN_KEY =
      "com.redhat.openshift.plugins.openshiftbamboo:openshiftBuildConfig";
  static final String BEARER = "Bearer ";
  static final String BINARY = "binary";
  static final String BINARY_CAP = "Binary";
  static final String BUILD = "build";
  static final String BUILDCONFIGS = "/buildconfigs/";
  static final Pattern BUILDCONFIG_NAME_PATTERN =
      Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*");
  static final String BUILDS = "/builds/";
  static final String BUILD_CONFIG_ALREADY_EXISTS = "BuildConfig already exists. ";
  static final String BUILD_CONFIG_SUCCESSFULLY_CREATED = "BuildConfig successfully created.";
  static final String BUILD_FINISHED_STATUS = "Build has finished with status \"";
  static final String BUILD_LOGS_AVAILABLE_AT = "Build logs available at ";
  static final String COMMA = ",";
  static final String COMPLETE = "Complete";
  static final String CONTAINERS = "containers";
  static final String CONTAINER_PORT = "containerPort";
  static final String CONTAINER_STORAGE = "container_storage";
  static final String CONTENT_TYPE = "Content-Type";
  static final String CREATING_TEMPLATE_JSON = "Creating template JSON.";
  static final String DC_PLUGIN_KEY =
      "com.redhat.openshift.plugins.openshiftbamboo:openshiftDeploymentConfig";
  static final String DEFAULT_ADMIN = "admin";
  static final String DEFAULT_BAMBOO_NAMESPACE = "bamboo-namespace";
  static final String DEFAULT_HTTPS_192_168_42_237_8443 = "https://192.168.42.237:8443";
  static final String DEFAULT_RUBY_22_CENTOS_7_LATEST = "ruby-22-centos7:latest";
  static final String DEFAULT_RUBY_EX = "ruby-ex";
  static final String DEFAULT_RUBY_EX_LATEST = "ruby-ex:latest";
  static final String DEPLOYMENTCONFIG = "deploymentconfig";
  static final String DEPLOYMENTCONFIGS = "/deploymentconfigs/";
  static final Pattern DEPLOYMENTCONFIG_NAME_PATTERN =
      Pattern.compile("(([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?");
  static final String DEPLOYMENT_SUCCESSFULLY_STARTED_WITH_REVISION =
      "Deployment successfully started with revision";
  static final String DIRECTLY_FROM_SOURCE_CODE = "...directly from source code";
  static final String DOCKER = "Docker";
  static final String DOCKERFILE = "dockerfile";
  static final String DOCKERIMAGE = "DockerImage";
  static final String DOCKER_IMAGE = "Docker image";
  static final String DOCKER_REPO = "docker_repo";
  static final String DOCKER_STRATEGY = "dockerStrategy";
  static final String EMPTYDIR = "emptydir";
  static final String EMPTY_DIR = "emptyDir";
  static final String EMPTY_STRING = "";
  static final String ENV = "env";
  static final String EQUALS = "=";
  static final String ERROR_DOWNLOADING_THE_ARTIFACT = "Error downloading the artifact";
  static final String ERROR_FETCHING_THE_ARTIFACT_URLS = "Error fetching the artifact URLs";
  static final String ERROR_GETTING_THE_ARTIFACT_URLS = "Error getting the artifact URLs";
  static final String ERROR_WHILE_CANCELLING_BUILD = "Error while cancelling build:";
  static final String ERROR_WHILE_CREATING_THE_BUILD_CONFIG =
      "Error while creating the BuildConfig.";
  static final String ERROR_WHILE_CREATING_THE_DEPLOYMENT_CONFIG =
      "Error while creating the deployment config.";
  static final String ERROR_WHILE_STARTING_THE_BUILD = "Error while starting the build.";
  static final String ERROR_WHILE_STARTING_THE_DEPLOYMENT = "Error while starting the deployment.";
  static final String EXISTING_BUILD_CONFIG_WILL_BE_OVERWRITTEN =
      "Existing build config will be overwritten.";
  static final String FAIL = "fail";
  static final String FAILED_TO_SET_UP_HTTP_CLIENT = "Failed to set up HTTP client.";
  static final String FAIL_THE_BUILD_PROCESS = "...fail the build process";
  static final String FIELD_EMPTY_ERROR = "Field cannot be empty.";
  static final String FROM = "from";
  static final String GIT = "git";
  static final String GIT_CAP = "Git";
  static final String HALTING_BUILD_CONFIG_CREATION = "Halting buildConfig creation.";
  static final String IGNORE = "ignore";
  static final String IGNORE_NEW_SETTINGS = "...ignore new settings";
  static final String IGNORING_NEW_BUILD_CONFIG_SETTINGS = "Ignoring new buildConfig settings. ";
  static final String IMAGE = "image";
  static final String IMAGESTREAM = "imagestream";
  static final String IMAGESTREAMTAG = "ImageStreamTag";
  static final String INSTANTIATE = "/instantiate";
  static final String INTERVAL_SECONDS = "intervalSeconds";
  static final String INVALID_BUILDCONFIG_NAME =
      "Invalid name. Buildconfig name must consist of "
          + "lower case alphanumeric characters, '-' or '.', and must start and end with an "
          + "alphanumeric character";
  static final String INVALID_CONTAINER_NAME = "Invalid container name.";
  static final String INVALID_DEPLOYMENTCONFIG_NAME =
      "Invalid name. Must consist of lower case alphanumeric characters, '-' or '.', "
          + "and must start and end with an alphanumeric character (e.g. 'example.com')";
  static final String INVALID_NAMESPACE_NAME =
      "Invalid name. Namespace must consist of lower "
          + "case alphanumeric characters or '-', and must start and end with an alphanumeric "
          + "character (e.g. 'my-name',  or '123-abc')";
  static final String INVALID_PORT_DEFINITION = "Invalid port definition.";
  static final String INVALID_STRATEGY = "Invalid strategy.";
  static final String IN_ARTIFACTORY = "...in Artifactory";
  static final String IN_A_GIT_REPOSITORY = "...in a Git repository";
  static final String KIND = "kind";
  static final String LABELS = "labels";
  static final String LATEST_VERSION = "latestVersion";
  static final String LOG_FOLLOW = "/log?follow";
  static final String MAX_SURGE = "maxSurge";
  static final String MAX_UNAVAILABLE = "maxUnavailable";
  static final String METADATA = "metadata";
  static final String MOUNT_PATH = "mountPath";
  static final String NAME = "name";
  static final String NAMESPACE = "namespace";
  static final String NAME_U = "name_";
  static final String NEW = "New";
  static final String OAPI_V_1_NAMESPACES = "/oapi/v1/namespaces/";
  static final String OCP_BUILD_ARTIFACT = "artifact";
  static final String OCP_BUILD_ARTIFACTORY = "artifactory";
  static final String OCP_BUILD_BAMBOO_ARTIFACT = "bamboo_artifact";
  static final String OCP_BUILD_DOCKERFILE = "dockerfile";
  static final String OCP_BUILD_GIT = "git";
  static final String OCP_BUILD_SOURCE = "source";
  static final String OCP_DEPLOYMENT_STRATEGY_RECREATE = "recreate";
  static final String OCP_DEPLOYMENT_STRATEGY_ROLLING = "rolling";
  static final String OCP_OUTPUT_DOCKER_REPO = "docker_repo";
  static final String OCP_OUTPUT_IMAGESTREAM = "imagestream";
  static final String OPENSHIFT_BUILDCONFIGS_LIST = "buildconfigs_list";
  static final String OPENSHIFT_BUILDCONFIG_EXISTS = "bcexists";
  static final String OPENSHIFT_BUILDCONFIG_EXISTS_STRATEGIES = "bcexists_list";
  static final String OPENSHIFT_BUILDCONFIG_NAME = "buildconfig_name";
  static final String OPENSHIFT_BUILDER_IMAGE_ARTIFACT_ARTIFACTORY =
      "builder_image_artifact_artifactory";
  static final String OPENSHIFT_BUILDER_IMAGE_ARTIFACT_BAMBOO = "builder_image_artifact_bamboo";
  static final String OPENSHIFT_BUILDER_IMAGE_ARTIFACT_GIT = "builder_image_artifact_git";
  static final String OPENSHIFT_BUILDER_IMAGE_DOCKERIFLE = "builder_image_dockerfile";
  static final String OPENSHIFT_BUILDER_IMAGE_GIT = "builder_image_git";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPES = "builder_image_types";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACTORY = "builder_image_type_artifactory";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPE_ARTIFACT_GIT = "builder_image_type_artifact_git";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPE_BAMBOO = "builder_image_type_bamboo";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPE_DOCKERFILE = "builder_image_type_dockerfile";
  static final String OPENSHIFT_BUILDER_IMAGE_TYPE_GIT = "builder_image_type_git";
  static final String OPENSHIFT_BUILD_ENV_VARS = "build_env_vars";
  static final String OPENSHIFT_BUILD_NAME = "build_name";
  static final String OPENSHIFT_BUILD_STRATEGIES = "buildstrategies";
  static final String OPENSHIFT_BUILD_STRATEGY = "buildstrategy";
  static final String OPENSHIFT_CONTAINER_IMAGE = "container_image_";
  static final String OPENSHIFT_CONTAINER_NAME = "container_name_";
  static final String OPENSHIFT_CONTAINER_PORTS = "container_ports_";
  static final String OPENSHIFT_CONTAINER_STORAGE_MOUNT = "container_storage_mount_";
  static final String OPENSHIFT_CONTAINER_STORAGE_MOUNTPOINT = "container_storage_mountpoint_";
  static final String OPENSHIFT_CONTAINER_STORAGE_NAME = "container_storage_name_";
  static final String OPENSHIFT_CONTAINER_STORAGE_PVC_NAME = "container_storage_pvc_name_";
  static final String OPENSHIFT_CONTAINER_STORAGE_TYPE = "container_storage_type_";
  static final String OPENSHIFT_CONTAINER_VARS = "container_vars_";
  static final String OPENSHIFT_DEPLOYMENTCONFIGS_LIST = "deploymentconfigs_list";
  static final String OPENSHIFT_DEPLOYMENTCONFIG_EXISTS = "dcexists";
  static final String OPENSHIFT_DEPLOYMENTCONFIG_EXISTS_STRATEGIES = "dcexists_list";
  static final String OPENSHIFT_DEPLOYMENTCONFIG_NAME = "deploymentconfig_name";
  static final String OPENSHIFT_DEPLOYMENTCONFIG_NAME_STRING = "deploymentconfig_name_string";
  static final String OPENSHIFT_DEPLOYMENT_REPLICAS = "replicas";
  static final String OPENSHIFT_DEPLOYMENT_STRATEGIES = "strategy_list";
  static final String OPENSHIFT_DEPLOYMENT_STRATEGY = "strategy";
  static final String OPENSHIFT_NAMESPACE = "namespace";
  static final Pattern OPENSHIFT_NAMESPACE_CONTAINER_PATTERN =
      Pattern.compile("[a-z0-9]([-a-z0-9]*[a-z0-9])?");
  static final String OPENSHIFT_OUTPUT_DOCKER_NAME = "output_docker_name";
  static final String OPENSHIFT_OUTPUT_IMAGESTREAM_NAME = "output_imagestream_name";
  static final String OPENSHIFT_OUTPUT_TYPE = "image_output";
  static final String OPENSHIFT_OUTPUT_TYPES = "image_outputs";
  static final String OPENSHIFT_PW = "password";
  static final String OPENSHIFT_REPOSITORY_ADDRESS = "repo_address";
  static final String OPENSHIFT_SERVER_ADDRESS = "server";
  static final String OPENSHIFT_STORAGETYPE_LIST = "storagetype_list";
  static final String OPENSHIFT_TOKEN = "token";
  static final String OPENSHIFT_USERNAME = "username";
  static final String OPEN_SHIFT = "[OpenShift] ";
  static final String OPEN_SHIFT_IMAGE_STREAM_TAG = "OpenShift image stream tag";
  static final String OUTPUT = "output";
  static final String OVERWRITE = "overwrite";
  static final String OVERWRITE_IT = "...overwrite it";
  static final String PATCHING_EXISTING_BUILD_CONFIG = "Patching existing build config. ";
  static final String PENDING = "Pending";
  static final String PERSISTENT_VOLUME_CLAIM = "persistentVolumeClaim";
  static final String PHASE = "phase";
  static final String PORTS = "ports";
  static final Pattern PORTS_PATTERN =
      Pattern.compile(
          "(([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])/(tcp|udp),)*(([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])/(tcp|udp))+",
          Pattern.CASE_INSENSITIVE);
  static final String PROTOCOL = "protocol";
  static final String PVC = "pvc";
  static final String READY_REPLICAS = "readyReplicas";
  static final String REASON = "reason";
  static final String RECREATE = "Recreate";
  static final String REPLICAS = "replicas";
  static final String REPLICATIONCONTROLLERS = "/replicationcontrollers/";
  static final String REPOSITORY_ADDRESS_ARTIFACT_ARTIFACTORY = "repo_address_artifact_artifactory";
  static final String REPOSITORY_ADDRESS_ARTIFACT_GIT = "repo_address_artifact_git";
  static final String REPOSITORY_ADDRESS_DOCKERFILE_GIT = "repo_address_dockerfile_git";
  static final String REPOSITORY_ADDRESS_SOURCE_GIT = "repo_address_source_git";
  static final String REQUEST_FAILED = "Request failed";
  static final String ROLLING = "Rolling";
  static final String ROLLING_PARAMS = "rollingParams";
  static final String RUNNING = "Running";
  static final String SELECTOR = "selector";
  static final String SLASH = "/";
  static final String SOURCE = "source";
  static final String SOURCE_CAP = "Source";
  static final String SOURCE_SECRET = "sourceSecret";
  static final String SOURCE_SECRET_ARTIFACT_GIT = "source_secret_artifact_git";
  static final String SOURCE_SECRET_DOCKERFILE_GIT = "source_secret_dockerfile_git";
  static final String SOURCE_SECRET_SOURCE_GIT = "source_secret_source_git";
  static final String SOURCE_STRATEGY = "sourceStrategy";
  static final String SPACE = " ";
  static final String SPEC = "spec";
  static final String STATUS = "status";
  static final String STRATEGY = "strategy";
  static final String TEMPLATE = "template";
  static final String TEXT_PLAIN = "text/plain";
  static final String THIS_CONTAINER_DOES_NOT_EXIST = "This container does not exist.";
  static final String TIMEOUT_SECONDS = "timeoutSeconds";
  static final String TO = "to";
  static final String TYPE = "type";
  static final String UPDATE_PERIOD_SECONDS = "updatePeriodSeconds";
  static final String URI = "uri";
  static final String USING_AN_ARTIFACT = "...using an artifact";
  static final String USING_A_DOCKERFILE = "...using a Dockerfile";
  static final String VALUE = "value";
  static final String VOLUMES = "volumes";
  static final String VOLUME_MOUNTS = "volumeMounts";
  static final ArrayList<Map<String, String>> runningBuilds = new ArrayList<>();

  private Constants() {
    throw new IllegalStateException("Utility class");
  }
}
