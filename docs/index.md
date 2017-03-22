# AEM VLT Sync

is a development tool to keep your local file system and AEM in sync: changes made at the local file system will flow (almost instantly) to the corresponding AEM instance and vice-versa!

Allowing you, as an AEM developer:

- to use your favorite editor/IDE to modify some JavaScript or JSP files and immediately check the results of your work at the browser window
- make quick changes using CRX DE (for instance, adjusting some CSS selector) and rest assured that your changes have been propagated to the file system


## Quick setup

1. Download the appropriate content package
   * for AEM 6.0 use [AEM VLT Sync 1.0.2](https://github.com/daniel-lima/aem-vltsync/releases/download/aem-vltsync-1.0.2/vltsync-content-1.0.2.zip)
   * for AEM 6.1 use [AEM VLT Sync 2.0.2](https://github.com/daniel-lima/aem-vltsync/releases/download/aem-vltsync-2.0.2/vltsync-content-2.0.2.zip)
   * for AEM 6.2 use [AEM VLT Sync 3.0.1](https://github.com/daniel-lima/aem-vltsync/releases/download/aem-vltsync-3.0.1/vltsync-content-3.0.1.zip)
2. Upload and install it through the [CRX Package Manager](http://localhost:4502/crx/packmgr/index.jsp)
3. Configure one or more instances of the registration component
   * access [OSGi -> Configuration in the AEM Web Console](http://localhost:4502/system/console/configMgr)
   * locate *VLT Sync Initial Registration* and click on *+*
   * fill in *Filter Roots* with an existent JCR path, like /apps/{your-project}
   * fill in *Local Path* with a path at your local file system, like /projects/{your-project}/content/src/main/jcr_root 
   * click on *Save*
4. Done!
   
The sync status can be found at .vlt-sync.log, under the *Local Path*.

For further information on installation and configuration, please refer to the Detailed Guide.


## Advantages over other tools

Because AEM VLT Sync is an installation and configuration wrapper for the [Vault Sync Service](http://jackrabbit.apache.org/filevault/usage.html#Vault_Sync), it:

* is very fast
* is fully integrated to the [FileVault Tool (VLT)](https://docs.adobe.com/docs/en/aem/6-0/develop/dev-tools/ht-vlttool.html?wcmmode=disabled) and the [Content Package Maven Plugin](https://docs.adobe.com/docs/en/aem/6-0/develop/dev-tools/vlt-mavenplugin.html?wcmmode=disabled)
* works seamlessly with preexistent META-INF/vault/filter.xml

Besides that, it

* doesn't require neither the installation or the configuration of "alien" 3rd party tools
* is capable of using preexistent .vlt-sync-config.properties and .vlt-sync-filter.xml, which gives you a finer control of whole process   
 
 
## Known limitations

The current version of Vault Sync Service doesn't operate over special vault serialized files (.content.xml, dialog.xml, ...). So, you'll continue to use VLT and Maven commands to update those type of files/nodes.
   
Care must be taken when using *Overwrite Config Files*=true in conjunction with *Sync Once Type*=*Auto detect* because they may cause the loss of data stored in the *Filter Roots*. 
