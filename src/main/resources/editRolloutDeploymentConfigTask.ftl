[@ui.bambooSection title="OpenShift settings" collapsible=true isCollapsed=false]

    [@credentialsMacro /]

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

[@ui.bambooSection title="Deployment config settings" collapsible=true isCollapsed=false]

  [@ww.select label="OpenShift deployment config" name="deploymentconfig_name" list='deploymentconfigs_list' listKey='id' listValue='configName+" [namespace: "+configNamespace+"]"' groupBy='taskUserDescription' cssClass="long-field"/]

  [@ui.messageBox type="warning" content="You need to define a deployment config first by using the OpenShift build config module." hidden=deploymentconfigs_list?has_content /]

  [@ui.messageBox type="error" content="An error occured while loading deployment configurations. " hidden=!deploymentconfig_load_error?has_content /]

[/@ui.bambooSection]

