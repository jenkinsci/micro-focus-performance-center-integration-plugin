![OpenText Logo](https://upload.wikimedia.org/wikipedia/commons/1/1b/OpenText_logo.svg)
# OpenText Enterprise Performance Engineering Integration with Git
This plugin integrates OpenText Enterprise Performance Engineering with Git via Jenkins.

---
**Table of Content**

  * [Versions](#versions)
    + [Version 1.0.0](#version-100)
    + [Version 1.1.0](#version-110)
    + [Version 1.1.1](#version-111)
    + [Version 1.1.2](#version-112)
    + [Version 1.1.3](#version-113)
    + [Version 1.1.4](#version-114)
    + [Version 1.1.5](#version-115)
    + [Version 1.1.6](#version-116)
    + [Version 1.1.7](#version-117)
    + [Version 1.1.8](#version-118)
    + [Downloads](#downloads)
  * [Prerequisites](#prerequisites)
  * [Synchronize OpenText Enterprise Performance Engineering With Git](#synchronize-openText-enterprise-performance-engineering-with-git)
  * [Run OpenText Enterprise Performance Engineering Test](#run-openText-enterprise-performance-engineering-test)

---
  
Using this plugin, you can:
- Upload performance scripts from a GitHub repository or a GitLab project to a project.
- Create performance tests from a YAML file.
- Run a performance test from its ID, or create it from a YAML file or YAML syntax and then run it.

There is a separate plugin for running performance tests using OpenText Enterprise Performance Engineering. For details, see [OpenText Application Automation Tools](https://wiki.jenkins.io/display/JENKINS/Micro+Focus+Application+Automation+Tools) - [OpenText Enterprise Performance Engineering Integration](https://wiki.jenkins.io/display/JENKINS/Performance+Center+Integration).

**Submit issues and feedback through [JIRA](https://issues.jenkins-ci.org/browse/JENKINS-36795?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20micro-focus-performance-center-integration)**

This plugin is open source. If you identify any issues or want to submit enhancement requests, please use JIRA. The component for this plugin is the micro-focus-performance-center-integration component. Your feedback and involvement will allow us to stabilize and enhance the capabilities of the plugin. The latest, early access, beta versions of this plugin are available here. 
## Versions

### Version 1.0.0
This version provides the following enhancements:

**Git synchronization**

- Ability to synchronize OpenText Enterprise Performance Engineering with Git by uploading performance scripts stored in a Git repository to a project.

### Version 1.1.0
This version provides the following enhancements:
- Git Synchronization: Ability to create a performance test from YAML files stored in Git.
- Test Execution: Ability to create a test (from a YAML file or YAML content) before running it.

### Version 1.1.1
Fixed a defect relating to a tech preview feature (REST API usage of Load Generator and Controller for dynamic provisioning).

### Version 1.1.2
This version provides the following enhancements:
- Network Virtualization Insights report is published in the Build's logs.
- Rebranded the product name from "Performance Center" to "LoadRunner Enterprise", "PC" to "LRE", and "My PC" to "LoadRunner Enterprise Application".

### Version 1.1.3
This version provides the following enhancements:
- Fix for Java 11.
- Connect to the server with a tenant.

### Version 1.1.4
This version provides the following enhancements:
- Fix for pipeline build (FreeStyle project works fine): 
  - problem: While the first build passes and synchronizes everything, subsequent builds, supposed to only synchronize the changes in the Git repository, cannot calculate the difference and fail.
  - Resolution: All pipeline builds now behave as if they are the first build and will synchronize everything.
- Added support for DevWeb protocol: A folder containing both the "main.js" and "rts.yml" files is considered a performance script.
- Added support for Gatling scripts: A folder containing a file with ".scala" for extension is considered a performance script.
- Additional rebranding (changing "Performance Center" to "LoadRunner Enterprise" in the build logs).

### Version 1.1.5
This version provides the following enhancements:
- Ability to use a token API (access key) associated with a user for authentication.
- Added support for Selenium script. A folder containing a file with ".java" extension in which the expression "import org.openqa.selenium" is detected will be considered performance script.
- Fixed a defect caused by the plugin trying to validate a Linux OS system path according to Windows OS criteria and vice versa, when the Jenkins server and the remote agent are on a different OS type.

### Version 1.1.6
This version provides the following enhancements:
- Rebranding from Micro Focus to OpenText.
- Possibility to associate a YAML defined test to a trend report.
- Possibility to search for a timeslot defined for performance test to execute and use it.
- Possibility to add a wait time between generation of the trend report and the download of the trend report to the build artifact.
- Timeslot duration can be parametrized via two parameters (Hours and Minutes).

### Version 1.1.7
This version provides the following enhancements:
- Removing unused dependency.

### Version 1.1.8
This version provides the following enhancements:
- Rebranding product name.
- Hiding confusing properties that remained for backward compatibility with unsupported old versions.
- Updating different dependency components for security compliance.
- New requirement: Jenkins 2.462.3 and above (requires java 17 or 21).

### Downloads
You can download the plugin from this link:

- [Version 1.0.0](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.0.0/micro-focus-performance-center-integration-1.0.0.hpi).
- [Version 1.1.0](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.0/micro-focus-performance-center-integration-1.1.0.hpi).
- [Version 1.1.1](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.1/micro-focus-performance-center-integration-1.1.1.hpi).
- [Version 1.1.2](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.2/micro-focus-performance-center-integration-1.1.2.hpi).
- [Version 1.1.3](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.3/micro-focus-performance-center-integration-1.1.3.hpi).
- [Version 1.1.4](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.4/micro-focus-performance-center-integration-1.1.4.hpi).
- [Version 1.1.5](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.5/micro-focus-performance-center-integration-1.1.5.hpi).
- [Version 1.1.6](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.6/micro-focus-performance-center-integration-1.1.6.hpi).
- [Version 1.1.7](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.7/micro-focus-performance-center-integration-1.1.7.hpi).
- [Version 1.1.8](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.8/micro-focus-performance-center-integration-1.1.8.hpi).

## Prerequisites
1. Jenkins 2.462.3 and above (and consequently Java version 17 or 21). To verify your Java version, go to http://www.java.com/en/download/installed.jsp.
2. OpenText Enterprise Performance Engineering server.
3. Git Repository on GitHub or GitLab server.
4. Jenkins versions: This plugin supports only the latest five LTS versions of Jenkins (currently, the latest is 2.479.3). This is a result of the Jenkins policy to no longer support older update centers. From Jenkins 2.462.3, Java 17 or 21 is required.
5. Content Security Policy: Starting with version 1.641 (or 1.625.3), Jenkins introduced the Content-Security-Policy header. This causes some of the integration links, such as links to reports, to become inoperable. For details, see [Configuring Content Security Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy) and [Jenkins Security Advisory](https://jenkins.io/security/advisory/2015-12-09/) . For suggested workarounds until the issue is resolved, see [Content Security Policy Header](https://wiki.jenkins.io/display/JENKINS/Micro+Focus+Application+Automation+Tools#MicroFocusApplicationAutomationTools-ContentSecurityPolicyHeader).

## Synchronize OpenText Enterprise Performance Engineering With Git
For details on how to perform this, see [Synchronize OpenText Enterprise Performance Engineering with Git](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Sync_PC_with_Git.htm) in the [OpenText Enterprise Performance Engineering help](https://admhelp.microfocus.com/lre).

## Run OpenText Enterprise Performance Engineering Test
Available in version 1.1.0. For details on how to perform this, see [Run OpenText Enterprise Performance Engineering test](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Run_test_using_PC.htm) in the [OpenText Enterprise Performance Engineering help](https://admhelp.microfocus.com/lre).

---
**NOTE** 

For details on this and other OpenText Enterprise Performance Engineering integrations, see the ["OpenText Enterprise Performance Engineering and Git"](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Git-integration.htm) and ["Plugins"](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Performance-Center-Plugins.htm) topics in the [OpenText Enterprise Performance Engineering help](https://admhelp.microfocus.com/lre).

---