
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

    [@ww.textfield label="OpenShift namespace" name="namespace" required='true' description="OpenShift namespace to deploy the buildconfig into." /]

[/#macro]

[@buildConfigMacro /]

[#macro buildConfigMacro]

    [@ui.bambooSection title="Build configuration settings" collapsible=true isCollapsed=false]

        [@ww.textfield label="Build config name" name="buildconfig_name"/]

        [@s.select label='I am building...' name='buildstrategy' listKey='key' listValue='value' toggle=true required=true list=buildstrategies /]

        [@ui.bambooSection dependsOn="buildstrategy" showOn="source"]

            [@ww.textfield label="Source code repository:" name="repo_address_source_git"  required='true' description="Address of Git repository to be used." /]

            [@ww.textfield label="Source secret name:" name="source_secret_source_git"  required='false'/]

            [@s.select label='Builder image type' name='builder_image_type_git' listKey='key' listValue='value' toggle=true required=true list=builder_image_types /]

            [@ww.textfield label="OpenShift builder image" name="builder_image_git" required='true' description="OpenShift builder image to use." /]

        [/@ui.bambooSection]

        [@ui.bambooSection dependsOn="buildstrategy" showOn="artifact"]

        [@s.select label='My artifact is...' name='artifactlocation' listKey='key' listValue='value' toggle=true required=true list=artifactlocations /]

             [@ui.bambooSection dependsOn="artifactlocation" showOn="git"]

                [@ww.textfield label="Git repository:" name="repo_address_artifact_git" description="Address of Git repository to be used." required=true/]

                [@ww.textfield label="Source secret name:" name="source_secret_artifact_git"  required='false' /]
                
		[@s.select label='Builder image type' name='builder_image_type_artifact_git' listKey='key' listValue='value' toggle=true required=true list=builder_image_types /]

                [@ww.textfield label="OpenShift builder image" name="builder_image_artifact_git" required='true' description="OpenShift builder image to use." /]

             [/@ui.bambooSection]

             [@ui.bambooSection dependsOn="artifactlocation" showOn="artifactory"]

                 [@ww.textfield label="Artifactory address:" name="repo_address_artifact_artifactory" description="URL to your artifact:" required=true/]

                 [@s.select label='Builder image type' name='builder_image_type_artifactory' listKey='key' listValue='value' toggle=true required=true list=builder_image_types /]

                 [@ww.textfield label="OpenShift builder image" name="builder_image_artifact_artifactory" required='true' description="OpenShift builder image to use." /]

             [/@ui.bambooSection]

             [@ui.bambooSection dependsOn="artifactlocation" showOn="bamboo_artifact"]

             [@s.select label='Builder image type' name='builder_image_type_bamboo' listKey='key' listValue='value' toggle=true required=true list=builder_image_types /]

             [@ww.textfield label="OpenShift builder image" name="builder_image_artifact_bamboo" required='true' description="OpenShift builder image to use." /]

             [@ui.messageBox content="The Bamboo artifact will be provided in the OpenShift Start Build task. "/]

             [/@ui.bambooSection]

        [/@ui.bambooSection]

        [@ui.bambooSection dependsOn="buildstrategy" showOn="dockerfile"]

            [@ww.textfield label="Source code repository:" name="repo_address_dockerfile_git" requred='true' description="Address of Git repository to be used." /]

            [@ww.textfield label="Source secret name:" name="source_secret_dockerfile_git"  required='false' /]
            
	    [@s.select label='Builder image type' name='builder_image_type_dockerfile' listKey='key' listValue='value' toggle=true required=true list=builder_image_types /]

            [@ww.textfield label="OpenShift builder image" name="builder_image_dockerfile" required='false' description="OpenShift builder image to use." /]

        [/@ui.bambooSection]

        [@ww.textfield label="Environment variables" name="build_env_vars" description="" required='false' /]

        [@s.select label='I am building into...' name='image_output' listKey='key' listValue='value' toggle=true required=true list=image_outputs /]

        [@ui.bambooSection dependsOn="image_output" showOn="imagestream"]

            [@ww.textfield label="Output ImageStream" name="output_imagestream_name" required='true' /]

        [/@ui.bambooSection]

        [@ui.bambooSection dependsOn="image_output" showOn="docker_repo"]

            [@ww.textfield label="Output Docker repository" name="output_docker_name" required='true' /]

        [/@ui.bambooSection]

    [/@ui.bambooSection]

[/#macro]

[@ui.bambooSection title="Advanced settings" collapsible=true isCollapsed=true]

    [@s.select label='If the build configuration already exists...' name='bcexists' listKey='key' listValue='value' toggle=true required=false list=bcexists_list /]

[/@ui.bambooSection]




