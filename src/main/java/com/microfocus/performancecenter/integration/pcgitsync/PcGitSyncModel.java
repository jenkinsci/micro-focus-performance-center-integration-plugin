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

/*
 * Takes all the parameter from the job in order to create a Sync object
 * */
package com.microfocus.performancecenter.integration.pcgitsync;

import com.microfocus.performancecenter.integration.pcgitsync.helper.UploadScriptMode;
import com.microfocus.performancecenter.integration.pcgitsync.helper.YesOrNo;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class PcGitSyncModel implements Serializable {


    private static final String EXPECTED_CREDENTIALSID_PARAMETER_NAME = "CREDENTIALSID";
    private static final String EXPECTED_CREDENTIALSPROXYID_PARAMETER_NAME = "CREDENTIALSPROXYID";

    private final String description;
    private final String pcServerName;
    private final String serverAndPort;
    private final boolean httpsProtocol;
    private final String credentialsId;
    private final String almDomain;
    private final String almProject;
    private final String proxyOutURL;
    private final String credentialsProxyId;
    private final String subjectTestPlan;
    private final UploadScriptMode uploadScriptMode;
    private final YesOrNo removeScriptFromPC;
    private final YesOrNo importTests;
    private final boolean authenticateWithToken;
    private String buildParameters;

    @DataBoundConstructor
    public PcGitSyncModel(String description, String pcServerName, String serverAndPort, boolean httpsProtocol,
                          String credentialsId, String almDomain, String almProject,
                          String proxyOutURL, String credentialsProxyId,
                          String subjectTestPlan, UploadScriptMode uploadScriptMode, YesOrNo removeScriptFromPC, YesOrNo importTests, boolean authenticateWithToken, String buildParameters) {
        this.description = description;
        this.pcServerName = pcServerName;
        this.serverAndPort = serverAndPort;
        this.httpsProtocol = httpsProtocol;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId = credentialsProxyId;
        if (subjectTestPlan != null
                && subjectTestPlan.length() > 0) {
            this.subjectTestPlan = subjectTestPlan.replace("/", "\\").replaceFirst("\\\\$", "").replaceAll("\\$", "");
        } else {
            this.subjectTestPlan = subjectTestPlan;
        }

        this.uploadScriptMode = uploadScriptMode;
        this.removeScriptFromPC = removeScriptFromPC;
        this.importTests = importTests;
        this.buildParameters = "";
        this.authenticateWithToken = authenticateWithToken;
    }

    public static List<UploadScriptMode> getUploadScriptModes() {
        return Arrays.asList(UploadScriptMode.values());
    }

    public static List<YesOrNo> getYesOrNo() {
        return Arrays.asList(YesOrNo.values());
    }

    private static String useParameterIfNeeded(String buildParameters, String attribute) {
        if (buildParameters != null && attribute != null && attribute.startsWith("$")) {
            String attributeParameter = attribute.replace("$", "").replace("{", "").replace("}", "");
            String[] buildParametersArray = buildParameters.replace("{", "").replace("}", "").split(",");
            for (String buildParameter : buildParametersArray) {
                if (buildParameter.trim().startsWith(attributeParameter + "=")) {
                    return buildParameter.trim().replace(attributeParameter + "=", "");
                }
            }
        }
        return attribute;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPcServerName() {
        return this.pcServerName;
    }

    public String getPcServerName(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.pcServerName) : getPcServerName();
    }

    public String getServerAndPort() {
        return this.serverAndPort;
    }

    public boolean getHttpsProtocol() {
        return this.httpsProtocol;
    }

    public String getAlmDomain() {
        return this.almDomain;
    }

    public String getAlmDomain(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.almDomain) : getAlmDomain();
    }

    public String getAlmProject() {
        return this.almProject;
    }

    public String getAlmProject(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.almProject) : getAlmProject();
    }

    public String getProxyOutURL() {
        return this.proxyOutURL;
    }

    public String getProxyOutURL(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, this.proxyOutURL) : getProxyOutURL();
    }

    public String getSubjectTestPlan() {
        if (this.subjectTestPlan != null && this.subjectTestPlan.length() > 0)
            return this.subjectTestPlan.substring(0, 1).toUpperCase() + this.subjectTestPlan.substring(1);
        else
            return this.subjectTestPlan;
    }

    public String getSubjectTestPlan(boolean fromPcClient) {
        return fromPcClient ? useParameterIfNeeded(buildParameters, getSubjectTestPlan()) : getSubjectTestPlan();
    }

    public String getBuildParameters() {
        return this.buildParameters;
    }

    public void setBuildParameters(String buildParameters) {
        this.buildParameters = buildParameters;
    }

    public String getCredentialsId() {

        return this.credentialsId;
    }

    public String getCredentialsId(boolean fromPcClient) {

        return fromPcClient ? useParameterForCredentialsIdIfNeeded(buildParameters, this.credentialsId, EXPECTED_CREDENTIALSID_PARAMETER_NAME) : getCredentialsId();
    }

    public String getCredentialsProxyId() {

        return this.credentialsProxyId;
    }

    public String getCredentialsProxyId(boolean fromPcClient) {

        return fromPcClient ? useParameterForCredentialsIdIfNeeded(buildParameters, this.credentialsProxyId, EXPECTED_CREDENTIALSPROXYID_PARAMETER_NAME) : getCredentialsProxyId();
    }

    public UploadScriptMode getUploadScriptMode() {

        return this.uploadScriptMode;
    }

    public YesOrNo getRemoveScriptFromPC() {
        return this.removeScriptFromPC;
    }

    public YesOrNo getImportTests() {
        return this.importTests;
    }

    @Override
    public String toString() {
        return String.format("%s", runParamsToString().substring(1));
    }

    public String runParamsToString() {
        return String.format("[PCServer='%s', HTTPSProtocol='%s', CredentialsId='%s', Domain='%s', Project='%s', " +
                        "proxy='%s', CredentialsProxyId='%s', subjectTestPlan = '%s', uploadScriptMode='%s', removeScriptFromPC='%s', importTests='%s', UseTokenForAuthentication= '%s']",
                pcServerName, httpsProtocol, credentialsId, almDomain, almProject,
                proxyOutURL, credentialsProxyId, subjectTestPlan, uploadScriptMode.getValue(), removeScriptFromPC.getValue(), importTests.getValue(), authenticateWithToken);
    }

    //public boolean getAuthenticateWithToken(){ return this.authenticateWithToken; }

    public String getProtocol() {
        if (!httpsProtocol)
            return "http";
        return "https";
    }

    public boolean isAuthenticateWithToken() {
        return this.authenticateWithToken;
    }

    private String useParameterForCredentialsIdIfNeeded(String buildParameters, String credentialsId, String expectedcredentialsIdParameterName) {
        if (buildParameters != null) {
            String[] buildParametersArray = buildParameters.replace("{", "").replace("}", "").split(",");
            for (String buildParameter : buildParametersArray) {
                if (buildParameter.trim().startsWith(expectedcredentialsIdParameterName + "=")) {
                    return buildParameter.trim().replace(expectedcredentialsIdParameterName + "=", "");
                }
            }
        }
        return credentialsId;
    }


}
