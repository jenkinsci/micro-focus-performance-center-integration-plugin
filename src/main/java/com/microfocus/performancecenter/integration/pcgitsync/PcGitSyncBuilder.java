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
 * Create the PcGitSyncModel and the PcGitSyncClient and allows the connection between the job and PC
 * */

package com.microfocus.performancecenter.integration.pcgitsync;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.google.inject.Injector;
import com.microfocus.performancecenter.integration.common.helpers.configuration.ConfigurationService;
import com.microfocus.performancecenter.integration.common.helpers.services.ModifiedFiles;
import com.microfocus.performancecenter.integration.common.helpers.utils.BuildParametersAndEnvironmentVariables;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;
import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;
import com.microfocus.performancecenter.integration.pcgitsync.helper.AbstractPcGitBuildStep;
import com.microfocus.performancecenter.integration.pcgitsync.helper.AbstractPcGitBuildStepDescriptor;
import com.microfocus.performancecenter.integration.pcgitsync.helper.UploadScriptMode;
import com.microfocus.performancecenter.integration.pcgitsync.helper.YesOrNo;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.*;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

public class PcGitSyncBuilder extends AbstractPcGitBuildStep<PcGitSyncBuilder.DescriptorImpl> implements SimpleBuildStep {

    public static final String ERROR = "Error";

    private transient UsernamePasswordCredentials usernamePCPasswordCredentials;
    private transient UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy;
    private PrintStream logger;
    private Run<?, ?> run;
    private final String description;
    private final String pcServerName;
    private final boolean httpsProtocol;
    private final String almDomain;
    private final String almProject;
    private final String serverAndPort;
    private final String proxyOutURL;
    private final String subjectTestPlan;
    private final UploadScriptMode uploadScriptMode;
    private final YesOrNo removeScriptFromPC;
    private final YesOrNo importTests;
    private final boolean authenticateWithToken;
    private String credentialsId;
    private String credentialsProxyId;
    private PcGitSyncModel pcGitSyncModel;
    private String buildParameters;
    private boolean addDate = true;
    private File WorkspacePath;


    @DataBoundConstructor
    public PcGitSyncBuilder(
            String description,
            String pcServerName,
            boolean httpsProtocol,
            String credentialsId,
            String almDomain,
            String almProject,
            String serverAndPort,
            String proxyOutURL,
            String credentialsProxyId,
            String subjectTestPlan,
            UploadScriptMode uploadScriptMode,
            YesOrNo removeScriptFromPC,
            YesOrNo importTests,
            boolean authenticateWithToken) {

        this.description = description;
        this.pcServerName = pcServerName;
        this.httpsProtocol = httpsProtocol;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.serverAndPort = serverAndPort;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId = credentialsProxyId;
        //turning first letter to upper case
        if (subjectTestPlan != null
                && subjectTestPlan.length() > "Subject\\".length()
                && subjectTestPlan.startsWith("Subject\\")) {
            this.subjectTestPlan = subjectTestPlan;
        } else {
            this.subjectTestPlan = "Subject\\scripts";
        }
        this.uploadScriptMode = uploadScriptMode;
        this.removeScriptFromPC = removeScriptFromPC;
        this.importTests = importTests;
        this.buildParameters = "";
        this.authenticateWithToken = authenticateWithToken;

        pcGitSyncModel =
                new PcGitSyncModel(
                        this.description.trim(),
                        this.pcServerName.trim(),
                        this.serverAndPort.trim(),
                        this.httpsProtocol,
                        this.credentialsId,
                        this.almDomain.trim(),
                        this.almProject.trim(),
                        this.proxyOutURL.trim(),
                        this.credentialsProxyId,
                        this.subjectTestPlan.trim(),
                        this.uploadScriptMode,
                        this.removeScriptFromPC,
                        this.importTests,
                        this.authenticateWithToken,
                        this.buildParameters);
    }

    public UsernamePasswordCredentials getUsernamePCPasswordCredentials() {
        return usernamePCPasswordCredentials;
    }

    public UsernamePasswordCredentials getCredentialsId(String credentialsId) {
        if (credentialsId != null && run != null)
            return getCredentialsById(credentialsId, run, logger, true);
        return null;
    }

    public UsernamePasswordCredentials getCredentialsProxyId(String credentialsProxyId) {
        if (credentialsProxyId != null && run != null)
            return getCredentialsById(credentialsProxyId, run, logger, false);
        return null;
    }

    private static UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger, boolean required) {
        if (StringUtils.isBlank(credentialsId)) {
            if (required)
                throw new NullPointerException("credentials is not configured.");
            else
                return null;
        }

        UsernamePasswordCredentials usernamePCPasswordCredentials = CredentialsProvider.findCredentialById(credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.create().build());

        if (usernamePCPasswordCredentials == null) {
            logger.println("Cannot find credentials with the credentialsId:" + credentialsId);
        }

        return usernamePCPasswordCredentials;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        FilePath workspace = build.getWorkspace(); // Extract workspace to a variable

        if (workspace == null) {
            WorkspacePath = null;
            return false; // Early return if the workspace is null
        }

        WorkspacePath = new File(workspace.toURI()); // Safe to use workspace as it's already checked

        PcGitSyncModel pcGitSyncModel = getPcGitSyncModel();
        if (pcGitSyncModel != null) {
            setPcGitSyncModelBuildParameters(build, listener);
        }

        perform(build, workspace, launcher, listener); // Proceed with perform method as workspace is not null
        return true;
    }


    public File getWorkspacePath() {
        return WorkspacePath;
    }

    private void setPcGitSyncModelBuildParameters(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
        BuildParametersAndEnvironmentVariables buildParametersAndEnvironmentVariables = new BuildParametersAndEnvironmentVariables(build, listener).invoke();
        String buildParametersAndEnvars = buildParametersAndEnvironmentVariables.getBuildParametersAndEnvars();
        if (!buildParametersAndEnvars.isEmpty())
            getPcGitSyncModel().setBuildParameters(buildParametersAndEnvars);
    }

    public String getAlmProject() {
        return getPcGitSyncModel().getAlmProject();
    }

    public String getAlmDomain() {
        return getPcGitSyncModel().getAlmDomain();
    }

    public PcGitSyncModel getPcGitSyncModel() {
        return pcGitSyncModel;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String newCredentialsId) {
        credentialsId = newCredentialsId;
    }

    @SuppressFBWarnings("FS_BAD_DATE_FORMAT_FLAG_COMBO")
    private String simpleDateFormater() {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E yyyy MMM dd 'at' HH:mm:ss.SSS a zzz");
            String simpleDate = simpleDateFormat.format(new Date());
            return simpleDate;
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    public void perform(@NonNull Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        run = build;
        logger = listener.getLogger();
        log(listener, "", addDate);

        try {
            String version = ConfigurationService.getVersion();
            if (!(version == null || version.equals("unknown")))
                log(listener, "plugin version is '%s'", addDate, version);
        } catch (IllegalStateException ex) {
            log(listener, "Error: IllegalStateException '%s'", addDate, ex.getMessage());
        }

        Set<ModifiedFile> modifiedFiles = getDescriptor()
                .getModifiedFiles()
                .getModifiedFilesSinceLastSuccess(listener, build, workspace.getRemote());

        usernamePCPasswordCredentials = getCredentialsId(credentialsId);
        usernamePCPasswordCredentialsForProxy = getCredentialsProxyId(credentialsProxyId);

        ConfigureSystemSection configureSystemSection = ConfigureSystemSection.get();

        PcGitSyncClient pcGitSyncClient = new PcGitSyncClient(
                listener,
                configureSystemSection,
                modifiedFiles,
                pcGitSyncModel,
                usernamePCPasswordCredentials,
                usernamePCPasswordCredentialsForProxy
        );
        Result result = Result.SUCCESS;
        try {
            Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
            if (jenkinsInstance != null) {
                Injector injector = jenkinsInstance.getInjector();
                if (injector != null) {
                    injector.injectMembers(pcGitSyncClient);
                } else {
                    log(listener, "Error: Jenkins injector is null. Cannot inject members.", addDate);
                    build.setResult(Result.FAILURE);  // Optional: Set the build result to FAILURE if needed.
                    return;
                }
            } else {
                log(listener, "Error: Jenkins instance is null. Cannot inject members.", addDate);
                build.setResult(Result.FAILURE);  // Optional: Set the build result to FAILURE if needed.
                return;
            }
            result = workspace.<Result>act(pcGitSyncClient);
        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            throw e;
        } catch (Exception ex) {
            result = Result.FAILURE;
            log(listener, "Error: '%s'", addDate, ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
        }
        build.setResult(result);
        log(listener, "", addDate);
    }

    private void provideStepResultStatus(Result resultStatus, Run<?, ?> build) {
        String runIdStr = "";
        logger.println(String.format("%s - Result Status%s: %s%s- - -",
                simpleDateFormater(),
                runIdStr,
                resultStatus.toString(),
                System.lineSeparator()));
        build.setResult(resultStatus);

    }

    public String getDescription() {
        return getPcGitSyncModel().getDescription();
    }

    public String getPcServerName() {
        return getPcGitSyncModel().getPcServerName();
    }

    public boolean isHttpsProtocol() {
        return getPcGitSyncModel().getHttpsProtocol();
    }

    public String getServerAndPort() {
        return getPcGitSyncModel().getServerAndPort();
    }

    public String getProxyOutURL() {
        return getPcGitSyncModel().getProxyOutURL();
    }

    public String getCredentialsProxyId() {
        return credentialsProxyId;
    }

    public String getSubjectTestPlan() {
        return getPcGitSyncModel().getSubjectTestPlan();
    }

    public UploadScriptMode getUploadScriptMode() {
        return getPcGitSyncModel().getUploadScriptMode();
    }

    public YesOrNo getRemoveScriptFromPC() {
        return getPcGitSyncModel().getRemoveScriptFromPC();
    }

    public YesOrNo getImportTests() {
        return getPcGitSyncModel().getImportTests();
    }

    public boolean isAuthenticateWithToken() {
        return getPcGitSyncModel().isAuthenticateWithToken();
    }

    //-----------------------------------------------------------------------------------------
    // This indicates to Jenkins that this is an implementation of an extension
    // point
    @Extension
    @Symbol("pcGitBuild")
//    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    public static final class DescriptorImpl extends AbstractPcGitBuildStepDescriptor {


        private transient ModifiedFiles modifiedFiles;

        @Override
        public String getDisplayName() {

            return Messages.displayName();
        }

        public FormValidation doCheckPcServerName(@QueryParameter String value) {

            return validateString(value, "LRE Server");
        }

        public FormValidation doCheckAlmUserName(@QueryParameter String value) {

            return validateString(value, "User name");
        }

        public FormValidation doCheckAlmDomain(@QueryParameter String value) {

            return validateString(value, "Domain");
        }

//        public FormValidation doCheckSubjectTestPlan(@QueryParameter String value) {
//
//            return validateString(value, "Test Plan folder");
//        }

        public FormValidation doCheckAlmProject(@QueryParameter String value) {

            return validateString(value, "Project");
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                                                   @QueryParameter String url,
                                                   @QueryParameter String value) {
            return CheckCredentialsId(project, url, value);
        }


        public FormValidation doCheckCredentialsProxyId(@AncestorInPath Item project,
                                                        @QueryParameter String url,
                                                        @QueryParameter String value) {
            return CheckCredentialsId(project, url, value);

        }

        private FormValidation CheckCredentialsId(Item project, String url, String urlValue) {
            if (project == null || !project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }

            String urlValueFixed = Util.fixEmptyAndTrim(urlValue);
            if (urlValueFixed == null) {
                return FormValidation.ok();
            }

            String urlFixesEmptiedAndTrimmed = Util.fixEmptyAndTrim(url);
            if (urlFixesEmptiedAndTrimmed == null)
            // not set, can't check
            {
                return FormValidation.ok();
            }

            if (urlFixesEmptiedAndTrimmed.indexOf('$') >= 0)
            // set by variable, can't check
            {
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                    URIRequirementBuilder.create().build(),
                    new IdMatcher(urlValueFixed))) {

                if (StringUtils.equals(urlValueFixed, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning("Cannot find any credentials with ID " + urlValueFixed);
        }


        /**
         * @param limitIncluded if true, value must be higher than limit. if false, value must be equal to or
         *                      higher than limit.
         */
        private FormValidation validateHigherThanInt(
                String valueToValidate,
                String field,
                int limit,
                boolean limitIncluded) {
            FormValidation ret = FormValidation.ok();
            String valueTrimmed = valueToValidate.trim();
            String messagePrefix = field + " must be ";
            if (StringUtils.isBlank(valueTrimmed)) {
                ret = FormValidation.error(messagePrefix + "set");
            } else {
                try {
                    //regular expression: parameter (with brackets or not)
                    if (valueTrimmed.matches("^\\$\\{[\\w-. ]*}$|^\\$[\\w-.]*$"))
                        return ret;
                        //regular expression: number
                    else if (valueTrimmed.matches("[0-9]*$|")) {
                        if (limitIncluded && Integer.parseInt(valueTrimmed) <= limit)
                            ret = FormValidation.error(messagePrefix + "higher than " + limit);
                        else if (Integer.parseInt(valueTrimmed) < limit)
                            ret = FormValidation.error(messagePrefix + "at least " + limit);
                    } else
                        ret = FormValidation.error(messagePrefix + "a whole number or a parameter, e.g.: 23, $TESTID or ${TEST_ID}.");
                } catch (Exception e) {
                    ret = FormValidation.error(messagePrefix + "a whole number or a parameter (e.g.: $TESTID or ${TestID})");
                }
            }

            return ret;

        }

        private FormValidation validateString(String valueToValidate, String field) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(valueToValidate.trim())) {
                ret = FormValidation.error(field + " must be set");
            }
            return ret;
        }

        /**
         * To fill in the credentials drop down list which's field is 'credentialsId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project,
                                                     @QueryParameter String credentialsId) {

            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(
                            project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                            project,
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.create().build())
                    .includeCurrentValue(credentialsId);
        }

        /**
         * To fill in the credentials drop down list which's field is 'credentialsProxyId'.
         * This method's name works with tag <c:select/>.
         */
        public ListBoxModel doFillCredentialsProxyIdItems(@AncestorInPath Item project,
                                                          @QueryParameter String credentialsId) {

            return doFillCredentialsIdItems(project, credentialsId);
        }

        public List<UploadScriptMode> getUploadScriptModes() {

            return PcGitSyncModel.getUploadScriptModes();
        }

        public List<YesOrNo> getYesOrNo() {

            return PcGitSyncModel.getYesOrNo();
        }


        public ModifiedFiles getModifiedFiles() {
            return this.modifiedFiles;
        }

        @Inject
        public void setModifiedFiles(ModifiedFiles modifiedFiles) {
            this.modifiedFiles = modifiedFiles;
        }
    }

}
