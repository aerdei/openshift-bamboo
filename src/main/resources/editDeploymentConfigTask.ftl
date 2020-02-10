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

[@ui.bambooSection title="Deployment config options" collapsible=true isCollapsed=false]

  [@ww.textfield label="Replicas" name="replicas" required='true' /]

  [@ww.select label="Deployment strategy" name="strategy" list='strategy_list' listKey='key' listValue='value' /]

[/@ui.bambooSection]

[#macro removeButton]
  <div class="aui-toolbar inline" >
    <ul class="toolbar-group">
      <li class="toolar-item" style="list-style:none;">
        <a class="toolbar-trigger">[@ww.text name="Remove" /]</a>
      </li>
    </ul>
  </div>
[/#macro]

[@containerSettingsMacro /]

[#macro containerElement index=0]

  [#if containerList.size() lt 2]

    [@ww.select label="Container image" name="container_image_${index}" description="Select container image" list='buildconfigs_list' listKey='id' listValue='buildConfigOutputName+" [namespace: "+configNamespace+"]"' groupBy='taskUserDescription' cssClass="long-field"/]

    [@ww.textfield label="Container ports" name="container_ports_${index}" description="Container ports, separated by comma. Port/Protocol: 8080/TCP" cssClass="long-field" /]

    [@ww.textfield label="Container name" name="container_name_${index}" description="Container name" required='true' /]

    [@ww.textfield label="Vars" name="container_vars_${index}" description="Vars" /]

  [#else]

    [@ww.select label="Container image" name="container_image_${index}" description="Select container image" list='buildconfigs_list' listKey='id' listValue='buildConfigOutputName+" [namespace: "+configNamespace+"]"' groupBy='taskUserDescription' cssClass="long-field"/]

    [@ww.textfield label="Container ports" name="container_ports_${index}" description="Container ports, separated by comma. Port/Protocol: 8080/TCP" cssClass="long-field" /]

    [@ww.textfield label="Container name" name="container_name_${index}" description="Container name"  required='true'/]

    [@ww.textfield label="Vars" name="container_vars_${index}" description="Vars" /]

  [/#if]

  [@removeButton /]

[/#macro]


[@containerStorageMacro /]

[#macro storageElement index=0]

     [#if storageList.size() lt 2]

    [@ww.select name="container_storage_type_${index}" label="Container storage type" list='storagetype_list' listKey='key' listValue='value' /]

    [@ww.textfield name="container_storage_name_${index}" label="Storage name" required='true' /]

    [@ui.bambooSection dependsOn="container_storage_type_${index}" showOn="pvc"]

      [@ww.textfield label="Persistent volume claim name" name="container_storage_pvc_name_${index}"  required='true' description="Name of the PVC to be used." /]

    [/@ui.bambooSection]

    [@ww.textfield name="container_storage_mount_${index}" label="Container name" required='true' /]

    [@ww.textfield name="container_storage_mountpoint_${index}" label="Storage mount point" required='true' /]

  [#else]

    [@ww.select name="container_storage_type_${index}" label="Container storage type" list='storagetype_list' listKey='key' listValue='value' /]

    [@ww.textfield name="container_storage_name_${index}" label="Storage name" required='true' /]

    [@ui.bambooSection dependsOn="container_storage_type_${index}" showOn="pvc"]

      [@ww.textfield label="Persistent volume claim name" name="container_storage_pvc_name_${index}"  required='true' description="Name of the PVC to be used." /]

    [/@ui.bambooSection]

    [@ww.textfield name="container_storage_mount_${index}" label="Container name" required='true' /]

    [@ww.textfield name="container_storage_mountpoint_${index}" label="Storage mount point" required='true' /]

    [/#if]

  [@removeButton /]

[/#macro]

[#macro containerSettingsMacro]

  [@ui.bambooSection title="Containers" collapsible=true isCollapsed=false]
  
    [@ui.messageBox type="warning" content="You need to define a buildconfig first by using the OpenShift build config module." hidden=buildconfigs_list?has_content /]
  
    [@ui.messageBox type="error" content="An error occured while loading build configurations." hidden=!buildconfig_load_error?has_content /]
  
      [#if buildconfigs_list?has_content]
  
        [@ww.textfield label="Deployment config name" name="deploymentconfig_name" required='true' /]
  
        <ul id="container-list">
  
          [#list containerList?sort as index]
    
            <li cont-id="${index}">[@containerElement index /]</li>
    
          [/#list]
  
        </ul>
  
        <a id=add-container>[@ww.text name="add_container" /]</a>
  
      [/#if]
  
  [/@ui.bambooSection]

[/#macro]

[#macro containerStorageMacro]

  [@ui.bambooSection title="Storage" collapsible=true isCollapsed=false] 

        <ul id="storage-list">
  
          [#list storageList?sort as index]
    
            <li stor-id="${index}">[@storageElement index /]</li>
    
          [/#list]
  
        </ul>
  
        <a id=add-storage>[@ww.text name="add_storage" /]</a>
  
  [/@ui.bambooSection]

[/#macro]

[@ui.bambooSection title="Advanced settings" collapsible=true isCollapsed=true]

  [@s.select label='If the deployment configuration already exists...' name='dcexists' listKey='key' listValue='value' toggle=true required=false list='dcexists_list' /]

[/@ui.bambooSection]

<script type="text/x-template" title="containerElementTemplate">

  [#assign containerElementInstance][@containerElement 869576137068/][/#assign]

  <li cont-id="{index}">${containerElementInstance?replace("869576137068","{index}")}</li>

</script>

<script type="text/x-template" title="containerStorageTemplate">

  [#assign containerStorageInstance][@storageElement 869576137068/][/#assign]

  <li stor-id="{index}">${containerStorageInstance?replace("869576137068","{index}")}</li>

</script>

<script type="text/javascript">

$ = AJS.$;
(function($, BAMBOO) {
    BAMBOO.CONTAINER = {};
    BAMBOO.CONTAINER.ContainerConfiguration = (function() {
        var defaults = {
                addContainerSelector: null,
                removeContainerSelector: '.toolbar-trigger',
                containerListSelector: null,
                containerSelector: null,
                idSelector: null,
                templates: {
                    containerListItem: null
                }
            },
            $list,
            options,
            addContainerItem = function() {
                var newIndex, $lastCheckout;
                $lastCheckout = $list.children(':last');
                newIndex = ($lastCheckout.length ? (parseInt($lastCheckout.attr(options.idSelector), 10) + 1) : 0);
                $(AJS.template.load(options.templates.containerListItem).fill({index: newIndex }).toString())
                    .hide().appendTo($list)
                    .find(options.containerSelector).end()
                    .slideDown();
                BAMBOO.DynamicFieldParameters.syncFieldShowHide($list);
            },
            removeContainerItem = function () {
                $(this).closest('.aui-toolbar').closest('li').slideUp(function () { $(this).remove(); });
            };
        return {
            init: function(opts) {
                options = $.extend(true, defaults, opts);
                $(function() {
                    $list = $(options.containerListSelector).delegate(options.removeContainerSelector, 'click', removeContainerItem);
                    $(options.addContainerSelector).click(addContainerItem);
                });
            }
        };
    }());
    BAMBOO.CONTAINER.StorageConfiguration = (function() {
        var defaults = {
                addStorageSelector: null,
                removeStorageSelector: '.toolbar-trigger',
                storageListSelector: null,
                storageSelector: null,
                idSelector: null,
                templates: {
                    storageListItem: null
                }
            },
            $list,
            options,
            addStorageItem = function() {
                var newIndex, $lastCheckout;
                $lastCheckout = $list.children(':last');
                newIndex = ($lastCheckout.length ? (parseInt($lastCheckout.attr(options.idSelector), 10) + 1) : 0);
                $(AJS.template.load(options.templates.storageListItem).fill({index: newIndex }).toString())
                    .hide().appendTo($list)
                    .find(options.storageSelector).end()
                    .slideDown();
                BAMBOO.DynamicFieldParameters.syncFieldShowHide($list);
            },
            removeStorageItem = function () {
                $(this).closest('.aui-toolbar').closest('li').slideUp(function () { $(this).remove(); });
            };
        return {
            init: function(opts) {
                options = $.extend(true, defaults, opts);
                $(function() {
                    $list = $(options.storageListSelector).delegate(options.removeStorageSelector, 'click', removeStorageItem);
                    $(options.addStorageSelector).click(addStorageItem);
                });
            }
        };
    }());
}(jQuery, window.BAMBOO = (window.BAMBOO || {})));
BAMBOO.CONTAINER.ContainerConfiguration.init({
            addContainerSelector: "#add-container",
            containerListSelector: "#container-list",
            containerSelector: 'select[name^="container_image_"]',
            idSelector: 'cont-id',
            templates: {
                containerListItem: "containerElementTemplate"
            }
});
BAMBOO.CONTAINER.StorageConfiguration.init({
            addStorageSelector: "#add-storage",
            storageListSelector: "#storage-list",
            storageSelector: 'select[name^="container_storage_"]',
            idSelector: 'stor-id',
            templates: {
                storageListItem: "containerStorageTemplate"
            }
});
</script>


<style>
#container-list {
  border-bottom: 1px solid #ddd;
  list-style: none;
  margin: 0 0 10px;
  padding: 0;
}
#container-list > li {
  margin: 0;
  padding-bottom: 10px;
  list-style: none;
  position: relative;
  columns:2;
  -webkit-columns: 2;
  -moz-columns: 2;
  display: inline-block;
  overflow: hidden
}
#container-list > li + li {
  border-top: 3px solid #ddd;
  padding-top: 10px;
  list-style: none;
  
}
#container-list .aui-toolbar {
  position: absolute;
  right: 0;
  top: 14px;
  z-index: 1;
}
#storage-list {
  border-bottom: 1px solid #ddd;
  list-style: none;
  margin: 0 0 10px;
  padding: 0;
}
#storage-list > li {
  padding-right:45px;
  margin: 0;
  padding-bottom: 10px;
  list-style: none;
  position: relative;
  display: inline-block;
  overflow: hidden
}
</style>

