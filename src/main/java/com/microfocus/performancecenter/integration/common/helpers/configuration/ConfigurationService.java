/*
 *  Certain versions of software accessible here may contain branding from Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.
 *  This software was acquired by Micro Focus on September 1, 2017, and is now offered by OpenText.
 *  Any reference to the HP and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE marks are the property of their respective owners.
 *
 * Copyright 2012-2023 Open Text
 *
 * The only warranties for products and services of Open Text and
 * its affiliates and licensors (“Open Text”) are as may be set forth
 * in the express warranty statements accompanying such products and services.
 * Nothing herein should be construed as constituting an additional warranty.
 * Open Text shall not be liable for technical or editorial errors or
 * omissions contained herein. The information contained herein is subject
 * to change without notice.
 *
 * Except as specifically indicated otherwise, this document contains
 * confidential information and a valid license is required for possession,
 * use or copying. If this work is provided to the U.S. Government,
 * consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 * Computer Software Documentation, and Technical Data for Commercial Items are
 * licensed to the U.S. Government under vendor's standard commercial license.
 */

package com.microfocus.performancecenter.integration.common.helpers.configuration;

import hudson.Plugin;
import jenkins.model.Jenkins;

/***
 * plugin configuration service -
 * 1. helps to change configuration
 * 2. helps to get configuration and model
 * 3. helps to get RestClient based on some configuration
 */
public class ConfigurationService {


    public static String getPluginVersion() {
        Plugin plugin = getJenkinsInstance().getPlugin("micro-focus-performance-center-integration");
        if (plugin == null)
            throw new IllegalStateException("failed to obtain plugin.");
        return plugin.getWrapper().getVersion();
    }

    public static String getVersion() throws IllegalStateException {
        String completeVersion = getPluginVersion();
        if (completeVersion != null) {
            String[] partsOfCompleteVersion = completeVersion.split(" [(]");
            return partsOfCompleteVersion[0];
        }
        return "unknown";
    }


    private static Jenkins getJenkinsInstance() {
        Jenkins result = Jenkins.getInstance();
        if (result == null) {
            throw new IllegalStateException("failed to obtain Jenkins instance");
        }
        return result;
    }


}
