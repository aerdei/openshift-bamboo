[@ui.bambooSection title="OpenShift settings" collapsible=true isCollapsed=false]

    [@credentialsMacro /]

    [@ww.textfield label="Artifactory URL" name="ARTIFACTORY_URL" required='true' description="Artifactory server URL" readonly=true hidden=true /]

    [@ww.textfield label="Artifactory token" name="ARTIFACTORY_TOKEN" required='true' description="Artifactory token" readonly=true hidden=true /]


[/@ui.bambooSection]

[#macro credentialsMacro]

    [#if OCP_TOKEN?has_content && OCP_SERVER?has_content]

        [@ww.textfield label="Server" name="OCP_SERVER" required='true' description="URL for authenticating with the server." readonly=true /]

        [@ww.textfield label="Token" name="OCP_TOKEN" required='true' description="Token for authenticating with the server." readonly=true /]

    [#else]

        [@ww.textfield label="OpenShift server" name="server" required='true' description="OpenShift server address." /]

        [@ww.textfield label="Username" name="username" required='true' description="Client username for authenticating with the server." /]

        [@ww.password label="Password" name="password" required='true' description="Client password for authenticating with the server." /]

    [/#if]

[/#macro]

[@ui.bambooSection title="Build config settings" collapsible=true isCollapsed=false]

  [@ww.select label="OpenShift build config" name="buildconfig_name" list='buildconfigs_list' listKey='id' listValue='configName+" [namespace: "+configNamespace+"]"' groupBy='taskUserDescription' cssClass="long-field"/]

  [@ui.messageBox type="warning" content="You need to define a buildconfig first by using the OpenShift build config module." hidden=buildconfigs_list?has_content /]

  [@ui.messageBox type="error" content="An error occured while loading build configurations. " hidden=!buildconfig_load_error?has_content /]

[/@ui.bambooSection]

