package com.redhat.openshift.plugins;

import static com.redhat.openshift.plugins.Constants.OPENSHIFT_BUILD_NAME;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_NAMESPACE;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_SERVER_ADDRESS;
import static com.redhat.openshift.plugins.Constants.OPENSHIFT_TOKEN;
import static com.redhat.openshift.plugins.Constants.runningBuilds;
import static org.apache.http.HttpStatus.SC_OK;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.event.BuildCanceledEvent;
import com.atlassian.bamboo.event.HibernateEventListenerAspect;
import com.atlassian.event.api.EventListener;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Map;

import javax.inject.Inject;

/**
 * BuildCancelledListener.java Purpose: Handle the event of a user stopping the build.
 *
 * @author Attila Erdei
 */
public class BuildCancelledListener {

  @ComponentImport private final BuildLoggerManager buildLoggerManager;

  @Inject
  public BuildCancelledListener(final BuildLoggerManager buildLoggerManager) {
    this.buildLoggerManager = buildLoggerManager;
  }

  /**
   * Method to use for handling an event of type BuildCancelledEvent. In the event of a
   * cancelled/stopped build, the currently running builds are patched with cancelled:true, leading
   * to the build process to be terminated.
   *
   * @param event the cancel event caught by the listener.
   */
  @SuppressWarnings("unused") // It IS used when cancelling builds.
  @EventListener
  @HibernateEventListenerAspect
  public void handleEvent(BuildCanceledEvent event) {
    if (event != null) {
      for (Map<String, String> runningBuild : runningBuilds) {
        try {
          HttpResponse<JsonNode> cancelBuildResponse =
              Unirest.patch(
                      runningBuild.get(OPENSHIFT_SERVER_ADDRESS)
                          + Constants.OAPI_V_1_NAMESPACES
                          + runningBuild.get(OPENSHIFT_NAMESPACE)
                          + Constants.BUILDS
                          + runningBuild.get(OPENSHIFT_BUILD_NAME))
                  .header(Constants.ACCEPT, Constants.APPLICATION_JSON_WILDCARD)
                  .header(
                      Constants.AUTHORIZATION, Constants.BEARER + runningBuild.get(OPENSHIFT_TOKEN))
                  .header(Constants.CONTENT_TYPE, Constants.APPLICATION_STRATEGIC_MERGE_PATCH_JSON)
                  .body("{\"status\":{\"cancelled\":true}}")
                  .asJson();
          if (cancelBuildResponse.getStatus() != SC_OK) {
            buildLoggerManager
                .getLogger(event.getPlanResultKey())
                .addErrorLogEntry(
                    Constants.OPEN_SHIFT
                        + Constants.ERROR_WHILE_CANCELLING_BUILD
                        + cancelBuildResponse.getStatus()
                        + cancelBuildResponse.getStatusText());
          }
        } catch (UnirestException uniEx) {
          buildLoggerManager
              .getLogger(event.getPlanResultKey())
              .addErrorLogEntry(Constants.OPEN_SHIFT + uniEx.getMessage());
        }
      }
    }
  }
}
