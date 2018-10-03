/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
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
        if(plugin == null)
            throw new IllegalStateException("failed to obtain plugin.");
        return plugin.getWrapper().getVersion();
    }

    public static String getVersion() throws IllegalStateException {
        String completeVersion = getPluginVersion();
        if(completeVersion != null) {
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
