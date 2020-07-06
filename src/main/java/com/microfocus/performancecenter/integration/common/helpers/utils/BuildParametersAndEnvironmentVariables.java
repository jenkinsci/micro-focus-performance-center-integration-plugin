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
        if(listener != null) {
            Map<String, String> buildEnvars = build.getEnvironment(listener);
            mapParamsAndEnvars.putAll(buildEnvars);
        }
        else
        {
            Map<String, String> buildEnvars = build.getEnvironment(new LogTaskListener(null, Level.INFO));
            mapParamsAndEnvars.putAll(buildEnvars);
        }
        buildParametersAndEnvars = mapParamsAndEnvars.toString();
        return this;
    }
}