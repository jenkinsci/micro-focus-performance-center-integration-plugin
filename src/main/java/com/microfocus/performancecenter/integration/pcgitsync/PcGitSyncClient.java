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

package com.microfocus.performancecenter.integration.pcgitsync;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcScript;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcTestPlanFolders;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.pcsubentities.test.Test;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.pcsubentities.test.content.common.Common;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.performancecenter.integration.common.helpers.compressor.Compressor;
import com.microfocus.performancecenter.integration.common.helpers.compressor.ICompressor;
import com.microfocus.performancecenter.integration.common.helpers.constants.PcTestRunConstants;
import com.microfocus.performancecenter.integration.common.helpers.services.WorkspaceScripts;
import com.microfocus.performancecenter.integration.common.helpers.services.WorkspaceTests;
import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFile;
import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFolder;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;
import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;
import com.microfocus.performancecenter.integration.pcgitsync.helper.ScriptUploader;
import com.microfocus.performancecenter.integration.pcgitsync.helper.UploadScriptMode;
import com.microfocus.performancecenter.integration.pcgitsync.helper.YesOrNo;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.security.Roles;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.remoting.RoleChecker;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.microfocus.performancecenter.integration.common.helpers.services.ModifiedFiles.initMessage;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

public class PcGitSyncClient implements FilePath.FileCallable<Result>, Serializable {
    private final TaskListener listener;
    private final ConfigureSystemSection configureSystemSection;
    @Nullable
    private final Set<ModifiedFile> modifiedFiles;
    private final PcGitSyncModel pcGitSyncModel;
    private final UsernamePasswordCredentials usernamePCPasswordCredentials;
    private final UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy;

    public PcGitSyncClient(TaskListener listener, ConfigureSystemSection configureSystemSection,
                           @Nullable Set<ModifiedFile> modifiedFiles, PcGitSyncModel pcGitSyncModel,
                           UsernamePasswordCredentials usernamePCPasswordCredentials,
                           UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy) {
        this.listener = listener;
        this.configureSystemSection = configureSystemSection;
        this.modifiedFiles = modifiedFiles;
        this.pcGitSyncModel = pcGitSyncModel;
        this.usernamePCPasswordCredentials = usernamePCPasswordCredentials;
        this.usernamePCPasswordCredentialsForProxy = usernamePCPasswordCredentialsForProxy;
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        rc.check(this, Roles.SLAVE);
    }

    @Override
    public Result invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        return start(workspace);
    }

    //main function
    public Result start(File workspace)
            throws IOException, InterruptedException {

        boolean deleteScripts = this.pcGitSyncModel.getRemoveScriptFromPC() == YesOrNo.YES;

        Result result = Result.SUCCESS;
        PcRestProxy restProxy = defineRestProxy();
        if (restProxy == null)
            return Result.FAILURE;

        boolean loggedIn = false;

        boolean allowFolderCreation;

        try {
            if (!validateParameters(listener)) {
                return Result.FAILURE;
            }

            Set<AffectedFolder> scriptsForDelete = null;
            Set<AffectedFolder> scriptsForUpload;
            Set<AffectedFile> testsToCreateOrUpdate = null;

            WorkspaceScripts wss = new WorkspaceScripts();
            WorkspaceTests wst = new WorkspaceTests();

            if (modifiedFiles == null) { // upload all scripts and all tests:

                scriptsForUpload = wss.getAllScriptsForUpload(workspace.toPath());
                if (pcGitSyncModel.getImportTests() != null && pcGitSyncModel.getImportTests().equals(YesOrNo.YES))
                    testsToCreateOrUpdate = wst.getAllTestsToCreateOrUpdate(workspace.toPath(), configureSystemSection.getDebug());
            } else { // upload/delete only deltas taken from the changelog:

                if (!modifiedFiles.isEmpty()) {
                    logSetOfChangedFiles(modifiedFiles);

                    Set<AffectedFolder> affectedFolders = wss.getAllAffectedFolders(modifiedFiles, workspace.toPath());

                    logSetOfAffectedScripts("List of folders modified in GIT repository since last successful build:", affectedFolders);

                    if (deleteScripts) {
                        scriptsForDelete = wss.getAllScriptsForDelete(modifiedFiles, workspace.toPath());
                        logSetOfAffectedScripts("List of scripts deleted from Git that will be deleted from the project:", scriptsForDelete);
                    }

                    scriptsForUpload = wss.getAllScriptsForUpload(affectedFolders, workspace.toPath());
                    logSetOfAffectedScripts("List of scripts added to Git that will be uploaded to the project:", scriptsForUpload);

                    if (pcGitSyncModel.getImportTests() != null && pcGitSyncModel.getImportTests().equals(YesOrNo.YES)) {
                        Set<AffectedFile> affectedFiles = wst.getAllAffectedFiles(modifiedFiles, workspace.toPath());
                        testsToCreateOrUpdate = wst.getAllTestsToCreateOrUpdate(affectedFiles, workspace.toPath(), configureSystemSection.getDebug());
                        logSetOfAffectedTests(testsToCreateOrUpdate);
                    }
                } else {
                    log(listener, "No files were modified since the last successful build", true);
                    return result;
                }
            }
            log(listener, "", true);

            initMessage(listener, "Beginning to sync between GIT repository and the project", true);

            loggedIn = login(restProxy);
            if (!loggedIn) {
                log(listener, "Login failed.", true);
                return Result.FAILURE;
            }
            Thread.sleep(50);
            log(listener, "The synchronization will be performed with the project '%s' on domain '%s'.",
                    true,
                    pcGitSyncModel.getAlmProject(),
                    pcGitSyncModel.getAlmDomain()
            );
            log(listener, "", false);

            allowFolderCreation = isAllowFolderCreation(restProxy);
            Thread.sleep(50);
            deleteScriptsFromPerformanceCenter(scriptsForDelete, restProxy, allowFolderCreation);
            Thread.sleep(50);
            result = result.combine(uploadScriptsToPerformanceCenter(scriptsForUpload, restProxy, allowFolderCreation));
            if (pcGitSyncModel.getImportTests() != null && pcGitSyncModel.getImportTests().equals(YesOrNo.YES)) {
                Thread.sleep(50);
                result = result.combine(createOrUpdateTestsInPerformanceCenter(testsToCreateOrUpdate, restProxy, allowFolderCreation));
            }
        } catch (PcException ex) {
            log(listener, "Error PcException: %s.", true, ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
            result = result.combine(Result.FAILURE);
        } catch (InterruptedException ex) {
            log(listener, "Job interruption exception: %s.", true, ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
            result = result.combine(Result.FAILURE);
        } catch (Exception ex) {
            log(listener, "General exception: %s.", true, ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
            result = result.combine(Result.FAILURE);
        } finally {
            logout(loggedIn, restProxy);
        }

        return result;
    }

    private PcRestProxy defineRestProxy() {

        String proxyOutUser = (usernamePCPasswordCredentialsForProxy == null || pcGitSyncModel.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getUsername();
        String proxyOutPassword = (usernamePCPasswordCredentialsForProxy == null || pcGitSyncModel.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getPassword().getPlainText();
        PcRestProxy restProxy = null;
        try {
            restProxy = new PcRestProxy(pcGitSyncModel.getProtocol(), pcGitSyncModel.getPcServerName(true), pcGitSyncModel.isAuthenticateWithToken(), pcGitSyncModel.getAlmDomain(true), pcGitSyncModel.getAlmProject(true), pcGitSyncModel.getProxyOutURL(true), proxyOutUser, proxyOutPassword);
        } catch (PcException e) {
            log(listener, String.format("Connection to PC server failed. Error: %s", e.getMessage()), true);
            logStackTrace(listener, configureSystemSection, e);
        }
        return restProxy;
    }


    private boolean login(PcRestProxy restProxy) {
        String pcUser = (usernamePCPasswordCredentials == null) ? "" : usernamePCPasswordCredentials.getUsername();
        String pcPassword = (usernamePCPasswordCredentials == null) ? "" : usernamePCPasswordCredentials.getPassword().getPlainText();

        boolean loggedIn = false;
        try {
            initMessage(listener, "Login to the server", false);

            if (pcGitSyncModel == null) {
                log(listener, "pcGitSyncModel is null", true);
            } else {
                log(
                        listener,
                        String.format("Login: Attempting to login to the server '%s://%s/LoadTest/%s' with credentials of %s '%s'",
                                pcGitSyncModel.getProtocol(),
                                restProxy.GetPcServer(),
                                restProxy.GetTenant(),
                                pcGitSyncModel.isAuthenticateWithToken() ? "ClientIdKey" : "User",
                                pcUser
                        ),
                        true
                );
            }
            loggedIn = restProxy.authenticate(pcUser, pcPassword);
        } catch (PcException e) {
            log(listener, String.format("Login error PcException: %s", e.getMessage()), true);
            logStackTrace(listener, configureSystemSection, e);
        } catch (Exception e) {
            log(listener, String.format("Login error Exception: %s", e.getMessage()), true);
            logStackTrace(listener, configureSystemSection, e);
        } finally {
            log(listener, String.format("Login: %s", loggedIn ? "succeeded" : "failed"), true);
            log(listener, "", false);
        }
        return loggedIn;
    }

    private boolean isAllowFolderCreation(PcRestProxy restProxy) {
        boolean allowFolderCreation = false;
        try {
            PcTestPlanFolders pcTestPlanFolders = restProxy.getTestPlanFolders();
            if (pcTestPlanFolders != null) {
                log(listener, "recent version detected.", true);
                log(listener, "", false);
                allowFolderCreation = true;
            }
        } catch (PcException | IOException ex) {
            log(listener, "Cannot retrieve Test Plan folder tree which means one of the following: ", true);
            log(listener, "- Performance Center version 12.57 or below is used and this means that folders in Test Plan tree are required to be created manually.", true);
            log(listener, "- Domain and Project details are wrong.", true);

        }
        return allowFolderCreation;
    }

    public void logout(boolean loggedIn, PcRestProxy restProxy) {
        if (!loggedIn)
            return;

        boolean logoutSucceeded;
        try {
            logoutSucceeded = restProxy.logout();
            log(listener, String.format("Logout: %s", logoutSucceeded ? "succeeded" : "failed"), true);
        } catch (PcException e) {
            log(listener, String.format("logout error PcException: %s. \n%s", e.getMessage(), Arrays.toString(e.getStackTrace())), true);
            logStackTrace(listener, configureSystemSection, e);
        } catch (Exception e) {
            log(listener, String.format("logout error Exception: %s. \n%s", e.getMessage(), Arrays.toString(e.getStackTrace())), true);
            logStackTrace(listener, configureSystemSection, e);
        }
    }


    private void deleteScriptsFromPerformanceCenter(
            Set<AffectedFolder> scriptsForDelete,
            PcRestProxy restProxy,
            boolean allowFolderCreation)
            throws IOException, PcException, InterruptedException {
        try {
            String subjectTestPlan = this.pcGitSyncModel.getSubjectTestPlan(true);

            if (scriptsForDelete == null || scriptsForDelete.isEmpty()) {
                return;
            }
            initMessage(listener, "Deleting scripts", false);
            try {
                Objects.requireNonNull(restProxy.getScripts());
            } catch (PcException | NullPointerException ex) {
                log(
                        listener, "An error occurred while getting the list of scripts from the project. Error: %s.",
                        true,
                        ex.toString()
                );
                logStackTrace(listener, configureSystemSection, ex);
            }
            for (AffectedFolder scriptForDelete : scriptsForDelete) {
                Thread.sleep(50);
                scriptToDelete(restProxy, allowFolderCreation, subjectTestPlan, scriptForDelete);
            }
            log(listener, "Finished deleting scripts", true);
            log(listener, "", true);
        } catch (InterruptedException ex) {
            log(listener, "job interrupted while deleting scripts", true);
            logStackTrace(listener, configureSystemSection, ex);
            throw ex;
        }
    }

    private void scriptToDelete(PcRestProxy restProxy, boolean allowFolderCreation, String subjectTestPlan, AffectedFolder localScript) {
        String targetSubject = allowFolderCreation ? localScript.getSubjectPath() : subjectTestPlan;
        Path localScriptRelativePath = localScript.getRelativePath();
        String localScriptName = localScriptRelativePath.getName(localScriptRelativePath.getNameCount() - 1).toString();
        try {
            log(
                    listener,
                    "Deleting script '%s\\%s' from the project...",
                    true,
                    targetSubject,
                    localScriptName
            );

            PcScript pcScriptToDelete = getScript(targetSubject, localScriptName, restProxy);
            if (pcScriptToDelete != null && pcScriptToDelete.getName().equalsIgnoreCase(localScriptName)) {
                deleteScript(restProxy, pcScriptToDelete);
            } else {
                log(
                        listener,
                        "---- This script was not found in the project, therefore it cannot be deleted.",
                        false
                );
            }
        } catch (PcException ex) {
            log(
                    listener,
                    "**** Could not verify if the script exists. Error PcException: %s.",
                    false,
                    ex.toString()
            );
            logStackTrace(listener, configureSystemSection, ex);
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            log(
                    listener,
                    "**** Could not verify if script exists. Error IOException: %s.",
                    false,
                    ex.toString()
            );
            logStackTrace(listener, configureSystemSection, ex);
            throw new RuntimeException(ex);
        } finally {
            log(
                    listener,
                    "",
                    false
            );
        }
    }

    private void deleteScript(PcRestProxy restProxy, PcScript pcScriptToDelete) {
        try {

            restProxy.deleteScript(pcScriptToDelete.getID());
            log(
                    listener,
                    "++++ Script deleted successfully.",
                    false
            );
        } catch (PcException | IOException ex) {
            log(
                    listener,
                    "**** Could not delete script. Error: %s",
                    false,
                    ex.getMessage()
            );
            logStackTrace(listener, configureSystemSection, ex);
            throw new RuntimeException(ex);
        }
    }

    public PcScript getScript(String testFolderPath, String scriptName, PcRestProxy restProxy) throws IOException, PcException {
        List<PcScript> pcScriptList = restProxy.getScripts().getPcScriptList();
        if (pcScriptList == null)
            return null;
        for (PcScript pcScript : pcScriptList
        ) {
            if (pcScript.getTestFolderPath().equalsIgnoreCase(testFolderPath.toLowerCase()) && pcScript.getName().equalsIgnoreCase(scriptName.toLowerCase())) {
                return pcScript;
            }
        }
        return null;
    }

    private Result createOrUpdateTestsInPerformanceCenter(Set<AffectedFile> testsToCreateOrUpdate,
                                                          PcRestProxy restProxy, boolean allowFolderCreation)
            throws PcException, InterruptedException {
        try {
            Result result = Result.SUCCESS;
            String subjectTestPlan = this.pcGitSyncModel.getSubjectTestPlan(true);
            if (testsToCreateOrUpdate == null || testsToCreateOrUpdate.isEmpty()) {
                return result;
            }
            uploadTestsInitialMessage();
            //for every script to add
            for (AffectedFile test : testsToCreateOrUpdate) {
                Thread.sleep(50);
                result = result.combine(createOrUpdateTest(restProxy, allowFolderCreation, result, subjectTestPlan, test));
            }
            log(listener, "Finished creating or updating tests step.", true);
            log(listener, "", false);
            return result;
        } catch (InterruptedException ex) {
            log(listener, "Job interrupted during createOrUpdateTestsInPerformanceCenter.", true);
            throw ex;
        }
    }

    private Result uploadScriptsToPerformanceCenter(Set<AffectedFolder> scriptsForUpload, PcRestProxy restProxy, boolean allowFolderCreation)
            throws PcException, InterruptedException, ExecutionException {
            Result result = Result.SUCCESS;

            String subjectTestPlan = this.pcGitSyncModel.getSubjectTestPlan(true);
            boolean uploadRunTimeFiles = this.pcGitSyncModel.getUploadScriptMode() == UploadScriptMode.RUNTIME_FILES;

            if (scriptsForUpload == null || scriptsForUpload.isEmpty()) {
                return result;
            }

            uploadScriptsInitialMessage();
            ICompressor compressor = new Compressor();


            ScriptUploader scriptUploader =
                    new ScriptUploader(listener,
                            configureSystemSection,
                            scriptsForUpload,
                            restProxy,
                            allowFolderCreation,
                            result,
                            subjectTestPlan,
                            uploadRunTimeFiles,
                            compressor);
            result = scriptUploader.uploadScripts();

            log(listener, "Finished uploading scripts step.", true);
            log(listener, "", false);
            return result;
    }

    private Result createOrUpdateTest(PcRestProxy restProxy, boolean allowFolderCreation, Result result,
                                      String subjectTestPlan, AffectedFile test)
            throws InterruptedException {
        Result resultToReturn = result;
        String testFullPath = test.getFullPath().toString();
        String ext = FilenameUtils.getExtension(testFullPath);
        boolean isXmlFile = PcTestRunConstants.XML_EXTENSION.substring(1).equalsIgnoreCase(ext);
        try {
            Thread.sleep(50);
            String targetSubject = allowFolderCreation ? test.getSubjectPath() : subjectTestPlan;
            String testFileContent = test.getTestContent();
            if (isXmlFile && (!testFileContent.toLowerCase().contains("<Test xmlns=\"http://www.hp.com/PC/REST/API".toLowerCase()) || !configureSystemSection.getDebug()))
                return resultToReturn;
            try {
                log(
                        listener,
                        "Creating or updating test '%s' from Git to the project",
                        true,
                        test.getRelativePath().toString().replace("/", "\\").concat("\\").concat(test.getFullPath().getFileName().toString())
                );
                Test createdTest = doCreateOrUpdateTest(restProxy, test, ext, isXmlFile, targetSubject, testFileContent);
                Thread.sleep(50);
                if (createdTest == null) {
                    log(
                            listener,
                            "----- Test was not created/updated",
                            false
                    );
                    return isXmlFile ? Result.SUCCESS : Result.FAILURE;
                }
                if (Common.stringToInteger(createdTest.getID()) > 0) {
                    log(
                            listener,
                            "+++++ Test created/updated successfully: '%s\\%s' (ID: %s).",
                            false,
                            createdTest.getTestFolderPath(),
                            createdTest.getName(),
                            createdTest.getID()
                    );
                } else {
                    resultToReturn = isXmlFile ? Result.SUCCESS : Result.FAILURE;
                    log(
                            listener,
                            "----- Failed to create/update the test.",
                            false
                    );
                }
                Thread.sleep(50);
            } catch (PcException ex) {
                resultToReturn = isXmlFile ? Result.SUCCESS : Result.FAILURE;
                log(
                        listener,
                        "***** Failed to create/update the test. Error: %s.",
                        false,
                        ex.getMessage()
                );
                logStackTrace(listener, configureSystemSection, ex);
            }
        } catch (IOException ex) {
            resultToReturn = isXmlFile ? Result.SUCCESS : Result.FAILURE;
            log(
                    listener,
                    "***** Failed to create/update the script. Error: %s.",
                    false,
                    ex.getMessage()
            );
            logStackTrace(listener, configureSystemSection, ex);
        } finally {
            log(
                    listener,
                    "",
                    false
            );
        }
        return resultToReturn;
    }

    private Test doCreateOrUpdateTest(PcRestProxy restProxy, AffectedFile test, String ext, boolean isXmlFile, String targetSubject, String testFileContent) throws IOException, PcException {
        Test createdTest = null;

        if (isXmlFile) {
            createdTest = restProxy.createOrUpdateTest(test.getTestName(), targetSubject, testFileContent);
        } else if (PcTestRunConstants.YAML_EXTENSION.substring(1).equalsIgnoreCase(ext) || PcTestRunConstants.YML_EXTENSION.substring(1).equalsIgnoreCase(ext))
            createdTest = restProxy.createOrUpdateTestFromYamlContent(test.getTestName(), targetSubject, testFileContent);
        return createdTest;
    }


    private void uploadScriptsInitialMessage() {
        initMessage(listener, "Uploading scripts", false);

        log(listener, "Each script folder will be automatically compressed in the workspace and then uploaded to the project:", false);
        log(listener, "", false);
    }

    private void uploadTestsInitialMessage() {
        initMessage(listener, "Creating or updating Tests", false);

        log(listener, String.format("Each test file (yaml%s) will be created or updated to the project:", configureSystemSection.getDebug() ? " or xml" : ""), false);
        log(listener, "", false);
    }

    private void logSetOfChangedFiles(Set<ModifiedFile> modifiedFiles) {
        log(listener, "", true);
        log(listener, "List of files modified in GIT repository since last successful build:", true);
        modifiedFiles.forEach(changedFile -> {
            log(listener, changedFile.toString(true), false);
        });
        log(listener, "", true);
    }

    private void logSetOfAffectedScripts(String header, Set<AffectedFolder> affectedFiles) {
        log(listener, header, true);

        if (affectedFiles.isEmpty()) {
            log(listener, "(None)", false);
            return;
        }

        affectedFiles.forEach(affectedFile -> {
            log(listener, affectedFile.toString(true), false);
        });
        log(listener, "", true);
    }

    private void logSetOfAffectedTests(Set<AffectedFile> affectedFiles) {
        log(listener, "List of tests added to Git that will be uploaded to the project:", true);

        if (affectedFiles.isEmpty()) {
            log(listener, "(None)", false);
            return;
        }

        affectedFiles.forEach(affectedFile -> {
            log(listener, affectedFile.toString(true), false);
        });
    }

    private boolean validateParameters(TaskListener listener) {

        String pcServerAndPort = this.pcGitSyncModel.getPcServerName(true);
        String domain = this.pcGitSyncModel.getAlmDomain(true);
        String project = this.pcGitSyncModel.getAlmProject(true);
        String subjectTestPlan = this.pcGitSyncModel.getSubjectTestPlan(true);
        String credentialsId = this.pcGitSyncModel.getCredentialsId(true);

        return verifyInputs(listener, pcServerAndPort, domain, project, subjectTestPlan, credentialsId);
    }

    private boolean verifyInputs(TaskListener listener, String pcServerAndPort, String domain, String project, String subjectTestPlan, String credentialsId) {
        String message = "";
        if (pcServerAndPort == null || pcServerAndPort.isEmpty()) {
            message = "Server not specified. ";
        }

        if (domain == null || domain.isEmpty()) {
            message = message.concat("No domain specified. ");
        }

        if (project == null || project.isEmpty()) {
            message = message.concat("No project specified. ");
        }

        if (subjectTestPlan == null || subjectTestPlan.isEmpty()) {
            message = message.concat("The path to the folder in the Test Plan is not specified. ");
        }

        if (subjectTestPlan != null && !subjectTestPlan.startsWith("Subject")) {
            message = message.concat(String.format("The path to the folder '%s' should start with 'Subject\\'. ", subjectTestPlan));
        }

        if (credentialsId == null) {
            message = message.concat("No credentials specified. ");
        }

        if (!message.isEmpty()) {
            listener.error(message);
            return false;
        }

        return true;
    }

    public TaskListener getListener() {
        return this.listener;
    }

    public ConfigureSystemSection getConfigureSystemSection() {
        return this.configureSystemSection;
    }

    @Nullable
    public Set<ModifiedFile> getModifiedFiles() {
        return this.modifiedFiles;
    }

    public PcGitSyncModel getPcGitSyncModel() {
        return this.pcGitSyncModel;
    }

    public UsernamePasswordCredentials getUsernamePCPasswordCredentials() {
        return this.usernamePCPasswordCredentials;
    }

    public UsernamePasswordCredentials getUsernamePCPasswordCredentialsForProxy() {
        return this.usernamePCPasswordCredentialsForProxy;
    }
}
