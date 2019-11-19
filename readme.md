![Micro Focus Logo](https://upload.wikimedia.org/wikipedia/commons/thumb/9/9a/Micro_Focus_logo.svg/220px-Micro_Focus_logo.svg.png)
# Micro Focus LoadRunner Enterprise Integration With Git
This plugin integrates Micro Focus LoadRunner Enterprise with Git via Jenkins.

Project status:
[![Build status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/micro-focus-performance-center-integration-plugin/master)](https://ci.jenkins.io/job/Plugins/job/micro-focus-performance-center-integration-plugin/job/master/)

---
**Table of Content**

  * [Versions](#versions)
    + [Version 1.0.0](#version-100)
    + [Version 1.1.0](#version-110)
    + [Version 1.1.1](#version-111)
    + [Version 1.1.2](#version-112)
    + [Downloads](#downloads)
  * [Prerequisites](#prerequisites)
  * [Synchronize LoadRunner Enterprise With Git](#synchronize-loadrunner-enterprise-with-git)
  * [Run Performance Test Using LoadRunner Enterprise](#run-performance-test-using-loadrunner-enterprise)

---
  
Using this plugin, you can:
- Upload LoadRunner and JMeter scripts from a GitHub repository or a GitLab project to a project on a LoadRunner Enterprise server.
- Create LoadRunner Enterprise tests from a YAML file.
- Run a LoadRunner Enterprise test from its ID or create it from YAML file or YAML syntax and then run it.

There is another plugin for running performance tests using LoadRunner Enterprise. For details, see [Micro Focus Application Automation Tools](https://wiki.jenkins.io/display/JENKINS/Micro+Focus+Application+Automation+Tools) - [LoadRunner Enterprise Integration](https://wiki.jenkins.io/display/JENKINS/Performance+Center+Integration).

**Submit issues and feedback through [JIRA](https://issues.jenkins-ci.org/browse/JENKINS-36795?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20micro-focus-performance-center-integration)**

This plugin is open source. If you identify any issues or want to submit enhancement requests, please use JIRA. The component for this plugin is the micro-focus-performance-center-integration component. Your feedback and involvement will allow us to stabilize and enhance the capabilities of the plugin. The latest, early access, beta versions of this plugin are available here. 
## Versions

### Version 1.0.0
This version provides the following enhancements:

**Git synchronization**

- Ability to synchronize LoadRunner Enterprise with Git by uploading LoadRunner and JMeter scripts stored in a Git repository to a LoadRunner Enterprise project.

### Version 1.1.0
This version provides the following enhancements:
- Git Synchronization: ability to create LoadRunner Enterprise test from yaml files stored in Git.
- Test Execution: Ability to create a test (from yaml file or from yaml content) before running it.

### Version 1.1.1
Fix for a defect related to a technology preview (REST API usage of LoadGenerator and Controllers dynamic provision).

### Version 1.1.2
This version provides the following enhancements:
- Network Virtualization Insights report to be publish in the Build's logs (works with LoadRunner Enterprise but requires a hotfix in all Performance Center versions).
- Rebranding from "Performance Center" product name to "LoadRunner Enterprise" product name, from "PC" acronym to "LRE" acronym, and from "My PC" expression to "LoadRunner Enterprise Application" expression.

### Downloads
You can download the plugin from this link:

- [Version 1.0.0](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.0.0/micro-focus-performance-center-integration-1.0.0.hpi).
- [Version 1.1.0](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.0/micro-focus-performance-center-integration-1.1.0.hpi).
- [Version 1.1.1](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.1/micro-focus-performance-center-integration-1.1.1.hpi).
- [Version 1.1.2](https://repo.jenkins-ci.org/releases/org/jenkins-ci/plugins/micro-focus-performance-center-integration/1.1.2/micro-focus-performance-center-integration-1.1.2.hpi).

## Prerequisites
1. Java version 8 or higher. To verify your Java version, go to http://www.java.com/en/download/installed.jsp.
2. LoadRunner Enterprise server.
3. Git Repository on GitHub or GitLab server.
4. Jenkins versions: This plugin supports only the latest five LTS versions of Jenkins (currently 2.60.3). This is a result of the Jenkins policy to no longer support older update centers. Jenkins 2.54 and higher requires you to have Java 8 installed on the machine.
5. Content Security Policy: Starting with version 1.641 (or 1.625.3), Jenkins introduced the Content-Security-Policy header. This causes some of the integration links, such as links to reports, to become inoperable. For details, see [Configuring Content Security Policy](https://wiki.jenkins.io/display/JENKINS/Configuring+Content+Security+Policy) and [Jenkins Security Advisory](https://jenkins.io/security/advisory/2015-12-09/) . For suggested workarounds until the issue is resolved, see [Content Security Policy Header](https://wiki.jenkins.io/display/JENKINS/Micro+Focus+Application+Automation+Tools#MicroFocusApplicationAutomationTools-ContentSecurityPolicyHeader).
6. LoadRunner Enterprise projects having Version Control enabled are not supported.

## Synchronize LoadRunner Enterprise With Git
For details on how to perform this, see [Synchronize LoadRunner Enterprise With Git](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Sync_PC_with_Git.htm) in the [LoadRunner Enterprise help](https://admhelp.microfocus.com/pc).

## Run Performance Test Using LoadRunner Enterprise
Available in version 1.1.0. For details on how to perform this, see [Run Performance Test Using LoadRunner Enterprise](https://admhelp.microfocus.com/pc/en/latest/online_help/Content/PC/Run_test_using_PC.htm) in the [LoadRunner Enterprise help](https://admhelp.microfocus.com/pc).

---
**NOTE** 

For details on this and other LoadRunner Enterprise integrations, see the ["LoadRunner Enterprise and Git"](https://admhelp.microfocus.com/pc/en/12.60-12.61/online_help/Content/PC/Git-integration.htm) and ["LoadRunner Enterprise plugins"](https://admhelp.microfocus.com/pc/en/12.60/online_help/Content/PC/Performance-Center-Plugins.htm) topics in the [LoadRunner Enterprise help](https://admhelp.microfocus.com/pc).

---