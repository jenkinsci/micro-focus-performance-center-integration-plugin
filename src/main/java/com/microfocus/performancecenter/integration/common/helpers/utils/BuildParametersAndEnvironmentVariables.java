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

package com.microfocus.performancecenter.integration.common.helpers.utils;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.LogTaskListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BuildParametersAndEnvironmentVariables {
    private AbstractBuild<?, ?> build;
    private BuildListener listener;
    private String buildParametersAndEnvars;

    public BuildParametersAndEnvironmentVariables(AbstractBuild<?, ?> build, BuildListener listener) {
        this.build = build;
        this.listener = listener;
    }

    public String getBuildParametersAndEnvars() {
        return buildParametersAndEnvars;
    }

    public BuildParametersAndEnvironmentVariables invoke() throws IOException, InterruptedException {
        Map<String, String> mapParamsAndEnvars = new HashMap<String, String>();
        Map<String, String> buildParameters = build.getBuildVariables();
        mapParamsAndEnvars.putAll(buildParameters);
        if (listener != null) {
            Map<String, String> buildEnvars = build.getEnvironment(listener);
            mapParamsAndEnvars.putAll(buildEnvars);
        } else {
            Map<String, String> buildEnvars = build.getEnvironment(new LogTaskListener(null, Level.INFO));
            mapParamsAndEnvars.putAll(buildEnvars);
        }
        buildParametersAndEnvars = mapParamsAndEnvars.toString();
        return this;
    }
}