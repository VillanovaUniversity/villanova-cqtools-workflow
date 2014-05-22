# Villanova CQ Tools

This is the CQ5.6 version. This repo contains tools for working with Adobe CQ workflows.

## Installation

Run `mvn clean package` to build the OSGI bundle. The jar file will be located in the /target folder of the module.

Run `mvn clean install -Pauto-deploy` to install the bundles into your local CQ instance with default admin username and password. If your instance has non-default credentials, or you want to deploy to another instance, add the following parameters using -D switches:

* crx.host
* crx.port
* crx.user
* crx.password

## Modules

### Workflow

#### Path Based Participant Chooser

Allows a workflow participant to be selected based on the payload path. This can be configured via the Configuration page in the CQ5 Web Console or a config file in the CRX.

##### Options
* payload.mappings (String[])
 * If the payload matches one of the paths defined, then the workflow is assigned to the group specified after the colon.
* default.group (String)
 * If the payload does not match any of the defined mappings, this group will be assigned the workflow. If this is empty, the workflow will default to the administrators group.

##### Example config file (_edu.villanova.cqtools.workflow.PathBasedParticipantChooser.xml_)
Should be placed in a location such as /apps/myapp/config/

    <?xml version="1.0" encoding="UTF-8"?>
        <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
            jcr:primaryType="sling:OsgiConfig"
            payload.mappings="[/content/myapp/about:about_group,/content/myapp/products:products_group]"
            default.group="administrators"/>

