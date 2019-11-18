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


/*
 *  Implements the main method of loadtest
 *
 * */

package com.microfocus.performancecenter.integration.pctestrun;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.pcsubentities.test.Test;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.performancecenter.integration.common.helpers.constants.PcTestRunConstants;
import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;
import hudson.FilePath;
import hudson.console.HyperlinkNote;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

public class PcTestRunClient {

    private PcTestRunModel model;
    private String testToCreate;
    private String testName;
    private String testFolderPath;
    private String fileExtension;
    private PcRestProxy restProxy;
    private boolean loggedIn;
    private TaskListener listener;
    private ConfigureSystemSection configureSystemSection;

    public PcTestRunClient(PcTestRunModel pcTestRunModel, String testToCreate, String testName, String testFolderPath, String fileExtension, TaskListener listener, ConfigureSystemSection configureSystemSection) {
        try {
            this.listener = listener;
            this.model = pcTestRunModel;
            this.testToCreate = testToCreate;
            this.testName = testName;
            this.testFolderPath = testFolderPath;
            this.fileExtension = fileExtension;
            this.configureSystemSection = configureSystemSection;
            String credentialsProxyId = model.getCredentialsProxyId(true);
            UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy = PcTestRunBuilder.getCredentialsId(credentialsProxyId);
            String proxyOutUser = (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getUsername();
            String proxyOutPassword= (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getPassword().getPlainText();
            if(model.getProxyOutURL(true) != null && !model.getProxyOutURL(true).isEmpty()) {
                log(listener, "%s: %s", true,  Messages.UsingProxy(),  model.getProxyOutURL(true));
                if(!proxyOutUser.isEmpty()) {
                    if (model.getCredentialsProxyId().startsWith("$"))
                        log(listener, "%s  %s.", true,  Messages.UsingProxyCredentialsBuildParameters(),proxyOutUser);
                    else
                        log(listener, "%s  %s.", true,  Messages.UsingProxyCredentialsConfiguration(), proxyOutUser);
                }
            }
            restProxy = new PcRestProxy(model.isHTTPSProtocol(), model.getPcServerName(true), model.getAlmDomain(true), model.getAlmProject(true), model.getProxyOutURL(true),proxyOutUser,proxyOutPassword);
        }catch (PcException e){
            log(listener, "%s: %s", true, Messages.Error(), e.getMessage());
        }

    }

    public <T extends PcRestProxy> PcTestRunClient(PcTestRunModel pcTestRunModel, /*PrintStream logger,*/ T proxy) {
        model = pcTestRunModel;
        restProxy = proxy;
        //this.logger = logger;
    }

    public boolean login(TaskListener listener) {
        try {
            this.listener = listener;
            String credentialsId = model.getCredentialsId(true);
            UsernamePasswordCredentials usernamePCPasswordCredentials = PcTestRunBuilder.getCredentialsId(credentialsId);
            log(listener,"",true);
            if(usernamePCPasswordCredentials != null) {
                if(model.getCredentialsId().startsWith("$"))
                    log(listener, "%s", true, Messages.UsingPCCredentialsBuildParameters());
                else
                    log(listener, "%s", true, Messages.UsingPCCredentialsConfiguration());
                log(listener, "%s\n[PCServer='%s://%s/LoadTest', User='%s']", true, Messages.TryingToLogin(), model.isHTTPSProtocol(), model.getPcServerName(true), usernamePCPasswordCredentials.getUsername());
                loggedIn = restProxy.authenticate(usernamePCPasswordCredentials.getUsername(), usernamePCPasswordCredentials.getPassword().getPlainText());
            }
            else {
                log(listener, "Performance Center credentials are missing.", true);
                loggedIn = false;
            }
        } catch (NullPointerException|PcException|IOException e) {
            log(listener, "%s: %s", true, Messages.Error(), e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
        }
        log(listener, "%s", true, loggedIn ? Messages.LoginSucceeded() : Messages.LoginFailed());
        return loggedIn;
    }

    public boolean isLoggedIn() {

        return loggedIn;
    }

    public int startRun() throws NumberFormatException, ClientProtocolException, PcException, IOException {

        int testID;
        log(listener,"",true);
        if("EXISTING_TEST".equals(model.getTestToRun())) {
            testID = getTestForExistingTestId();
        }
        else {
            Test test = createTestFromYamlOrXml();
            if(test == null) {
                log(listener, "Could not create test from yaml.", true);
                return 0;
            }
            testID = Integer.parseInt(test.getID());
            model.setTestId(test.getID());
            log(listener, "Running YAML test: Test ID %s, Name: %s, Path: %s", true, test.getID(), test.getName(), test.getTestFolderPath());
        }
        log(listener,"",true);
        int testInstance = getCorrectTestInstanceID(testID);
        setCorrectTrendReportID();
        printInitMessage(testInstance);
        PcRunResponse response = null;
        try {
            response = restProxy.startRun(testID,
                    testInstance,
                    model.getTimeslotDuration(),
                    model.getPostRunAction().getValue(),
                    model.isVudsMode());
            log(listener, "%s (TestID: %s, RunID: %s, TimeslotID: %s)", true, Messages.RunStarted(), response.getTestID(), response.getID(), response.getTimeslotID());

            return response.getID();
        } catch (NumberFormatException|ClientProtocolException|PcException ex) {
            log(listener, "%s. %s: %s", true, Messages.StartRunFailed(), Messages.Error(), ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
        } catch (IOException ex) {
            log(listener, "%s. %s: %s", true, Messages.StartRunFailed(), Messages.Error(), ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
        }
        if (!("RETRY".equals(model.getRetry()))) {
            return 0;
        }
        else {
            //counter
            int retryCount = 0;
            //values
            int retryDelay = Integer.parseInt(model.getRetryDelay());
            int retryOccurrences = Integer.parseInt(model.getRetryOccurrences());

            while (retryCount<=retryOccurrences) {
                retryCount++;
                try {
                    if(retryCount <= retryOccurrences) {
                        log(listener, "%s. %s (%s %s). %s: %s.", true,
                                Messages.StartRunRetryFailed(),
                                Messages.AttemptingStartAgainSoon(),
                                retryDelay,
                                Messages.Minutes(),
                                Messages.AttemptsRemaining(),
                                retryOccurrences - retryCount + 1);
                        Thread.sleep(retryDelay * 60 * 1000);
                    }
                } catch (InterruptedException ex) {
                    log(listener, "wait interrupted", true);
                    logStackTrace(listener, configureSystemSection, ex);
                    return 0;
                }

                response = startRunAgain(testID, testInstance, response);
                int ret = (response !=null) ? response.getID() : 0;
                if (ret != 0) {
                    log(listener, "%s (TestID: %s, RunID: %s, TimeslotID: %s))", true,
                            Messages.RunStarted(),
                            response.getTestID(),
                            response.getID(),
                            response.getTimeslotID());
                }
                return ret;
            }
        }
        return 0;
    }

    private PcRunResponse startRunAgain(int testID, int testInstance, PcRunResponse response) {
        try {
            response = restProxy.startRun(testID,
                    testInstance,
                    model.getTimeslotDuration(),
                    model.getPostRunAction().getValue(),
                    model.isVudsMode());
        } catch (NumberFormatException| ClientProtocolException | PcException ex) {
            log(listener, "%s. %s: %s", true,
                    Messages.StartRunRetryFailed(),
                    Messages.Error(),
                    ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
        } catch (IOException ex) {
            log(listener, "%s. %s: %s", true,
                    Messages.StartRunRetryFailed(),
                    Messages.Error(),
                    ex.getMessage());
            logStackTrace(listener, configureSystemSection, ex);
        }
        return response;
    }

    private void printInitMessage(int testInstance) {
        log(listener, "\n%s \n" +
                        "====================\n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "%s: %s \n" +
                        "====================\n",
                false,
                Messages.ExecutingLoadTest(),
                Messages.Domain(), model.getAlmDomain(true),
                Messages.Project(), model.getAlmProject(true),
                Messages.TestID(), Integer.parseInt(model.getTestId(true)),
                Messages.TestInstanceID(), testInstance,
                Messages.TimeslotDuration(), model.getTimeslotDuration(),
                Messages.PostRunAction(), model.getPostRunAction().getValue(),
                Messages.UseVUDS(), model.isVudsMode());
    }

    private Test createTestFromYamlOrXml() throws IOException, PcException {
        Test test = null;
        if (testName.isEmpty())
            test = restProxy.createOrUpdateTestFromYamlTest(testToCreate);
        else {
            switch (fileExtension.toLowerCase()) {
                case PcTestRunConstants.XML_EXTENSION:
                    test = restProxy.createOrUpdateTest(testName, testFolderPath, testToCreate);
                    break;
                case PcTestRunConstants.YAML_EXTENSION:
                case PcTestRunConstants.YML_EXTENSION:
                    test = restProxy.createOrUpdateTestFromYamlContent(testName, testFolderPath, testToCreate);
                    break;
                default:
                    log(listener, "File extension not supported.", true);
                    break;
            }
        }
        return test;
    }

    private int getTestForExistingTestId() throws IOException, PcException {
        int testID = Integer.parseInt(model.getTestId(true));
        Test test = restProxy.getTest(testID);
        log(listener, "Running existing test: Test ID %s, Name: %s, Path: %s", true, test.getID(), test.getName(), test.getTestFolderPath());
        return testID;
    }

    private int getCorrectTestInstanceID(int testID) throws IOException, PcException {
        if("AUTO".equals(model.getAutoTestInstanceID())){
            try {


                log(listener, Messages.SearchingTestInstance(), true);
                PcTestInstances pcTestInstances = null;
                try {
                    pcTestInstances = restProxy.getTestInstancesByTestId(testID);
                } catch (PcException ex) {
                    log(listener, "%s - getTestInstancesByTestId %s. Error: %s", true,
                            Messages.Failure(),
                            Messages.Error(),
                            ex.getMessage());
                }

                int testInstanceID;
                if (pcTestInstances != null && pcTestInstances.getTestInstancesList() != null){
                    PcTestInstance pcTestInstance = pcTestInstances.getTestInstancesList().get(pcTestInstances.getTestInstancesList().size()-1);
                    testInstanceID = pcTestInstance.getInstanceId();
                    log(listener, "%s: %s", true,
                            Messages.FoundTestInstanceID(),
                            testInstanceID);
                }else{
                    log(listener, Messages.NotFoundTestInstanceID(), true);
                    log(listener, Messages.SearchingAvailableTestSet(), true);
                    // Get a random TestSet
                    PcTestSets pcTestSets = restProxy.GetAllTestSets();
                    if (pcTestSets !=null && pcTestSets.getPcTestSetsList() !=null){
                        PcTestSet pcTestSet = pcTestSets.getPcTestSetsList().get(pcTestSets.getPcTestSetsList().size()-1);
                        int testSetID = pcTestSet.getTestSetID();
                        log(listener, "%s (Test ID: %s, TestSet ID: %s", true,
                                Messages.CreatingNewTestInstance(),
                                testID,
                                testSetID);
                        testInstanceID = restProxy.createTestInstance(testID,testSetID);
                        log(listener, "%s: %s", true,
                                Messages.TestInstanceCreatedSuccessfully(),
                                testInstanceID);
                    } else {
                        String msg = Messages.NoTestSetAvailable();
                        log(listener, "%s: %s", true,
                                Messages.Error(),
                                msg);
                        throw new PcException(msg);
                    }
                }
                return testInstanceID;
            } catch (Exception e){
                log(listener, "getCorrectTestInstanceID %s. %s: %s", true,
                        Messages.Failure(),
                        Messages.Error(),
                        e.getMessage());
                logStackTrace(listener, configureSystemSection, e);
                return Integer.parseInt(null);
            }
        }
        return Integer.parseInt(model.getTestInstanceId(true));
    }

    private void setCorrectTrendReportID() throws IOException, PcException {
        // If the user selected "Use trend report associated with the test" we want the report ID to be the one from the test
        String msg = Messages.NoTrendReportAssociated() + "\n" +
                Messages.PleaseTurnAutomaticTrendOn() + "\n" +
                Messages.PleaseTurnAutomaticTrendOnAlternative();
        if (("ASSOCIATED").equals(model.getAddRunToTrendReport()) && model.getPostRunAction() != PostRunAction.DO_NOTHING) {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
            //if the trend report ID is parametrized
            if(!model.getTrendReportId().startsWith("$")) {
                if (pcTest.getTrendReportId() > -1)
                    model.setTrendReportId(String.valueOf(pcTest.getTrendReportId()));
                else {
                    throw new PcException(msg);
                }
            }
            else {
                try {
                    if (Integer.parseInt(model.getTrendReportId(true)) > -1)
                        model.setTrendReportId(String.valueOf(model.getTrendReportId(true)));
                    else {
                        throw new PcException(msg);
                    }
                }
                catch (Exception ex) {
                    logStackTrace(listener, configureSystemSection, ex);
                    throw new PcException(msg + System.getProperty("line.separator") + ex);
                }
            }
        }
    }

    public String getTestName()  throws IOException, PcException{

        try {
            PcTest pcTest = restProxy.getTestData(Integer.parseInt(model.getTestId(true)));
            return pcTest.getTestName();
        }
        catch (PcException|IOException ex) {
            log(listener, "getTestData failed for testId : %s", true, model.getTestId(true));
            logStackTrace(listener, configureSystemSection, ex);
            throw ex;
        }
    }

    public PcRunResponse waitForRunCompletion(int runId) throws InterruptedException, ClientProtocolException, PcException, IOException {

        return waitForRunCompletion(runId, 5000);
    }

    public PcRunResponse waitForRunCompletion(int runId, int interval) throws InterruptedException, ClientProtocolException, PcException, IOException {
        RunState state;
        switch (model.getPostRunAction()) {
            case DO_NOTHING:
                state = RunState.BEFORE_COLLATING_RESULTS;
                break;
            case COLLATE:
                state = RunState.BEFORE_CREATING_ANALYSIS_DATA;
                break;
            case COLLATE_AND_ANALYZE:
                state = RunState.FINISHED;
                break;
            default:
                state = RunState.UNDEFINED;
                break;
        }
        return waitForRunState(runId, state, interval);
    }


    private PcRunResponse waitForRunState(int runId, RunState completionState, int interval) throws InterruptedException,
            ClientProtocolException, PcException, IOException {

        int counter = 0;
        RunState[] states = {RunState.BEFORE_COLLATING_RESULTS,RunState.BEFORE_CREATING_ANALYSIS_DATA};
        PcRunResponse response = null;
        RunState lastState = RunState.UNDEFINED;
        int threeStrikes = 3;
        do {
            try {

                if (threeStrikes < 3) {
                    log(listener, "Cannot get response from PC about the state of RunID: %s %s time(s) consecutively", true,
                            runId,
                            (3 - threeStrikes));
                    if(threeStrikes==0) {
                        log(listener, "%s: %s", true,
                                Messages.StoppingMonitoringOnRun(),
                                runId);
                        break;
                    }
                    Thread.sleep(2000);
                    login(listener);
                }
                response = restProxy.getRunData(runId);
                RunState currentState = RunState.get(response.getRunState());
                if (lastState.ordinal() < currentState.ordinal()) {
                    lastState = currentState;
                    log(listener, "RunID: %s - State = %s", true,
                            runId,
                            currentState.value());
                }

                // In case we are in state before collate or before analyze, we will wait 1 minute for the state to change otherwise we exit
                // because the user probably stopped the run from PC or timeslot has reached the end.
                if (Arrays.asList(states).contains(currentState)) {
                    counter++;
                    Thread.sleep(1000);
                    if (counter > 60) {
                        log(listener, "Run ID: %s  - %s = %s", true,
                                runId,
                                Messages.StoppedFromPC(),
                                currentState.value());
                        break;
                    }
                } else {
                    counter = 0;
                    Thread.sleep(interval);
                }
                threeStrikes = 3;
            }catch(PcException e) {
                threeStrikes--;
            }
            catch(InterruptedException e)
            {
                log(listener, "Job execution interrupted: %s", true,
                        runId,
                        e.getMessage());
                break;
            }
        } while (lastState.ordinal() < completionState.ordinal());
        return response;
    }

    public FilePath publishRunReport(int runId, String reportDirectory) throws IOException, PcException, InterruptedException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null){
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(PcTestRunBuilder.pcReportArchiveName)) {
                    FilePath reportFile = getFilePath(runId, reportDirectory, result, false);
                    if (reportFile != null) return reportFile;
                }
            }
        }
        log(listener, Messages.FailedToGetRunReport(), true);
        return null;
    }

    public FilePath publishRunNVInsightsReport(int runId, String reportDirectory) throws IOException, PcException, InterruptedException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null){
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(PcTestRunBuilder.pcNVInsightsReportArchiveName)) {
                    FilePath reportFile = getFilePath(runId, reportDirectory, result, true);
                    if (reportFile != null) return reportFile;
                }
            }
        }
        //log(listener, Messages.FailedToGetRunNVInsightsReport(), true);
        return null;
    }

    private FilePath getFilePath(int runId, String reportDirectory, PcRunResult result, boolean nvInsights) throws IOException, PcException, InterruptedException {
        File dir = new File(reportDirectory);
        dir.mkdirs();
        String reportArchiveFullPath = dir.getCanonicalPath() + IOUtils.DIR_SEPARATOR + (nvInsights ? PcTestRunBuilder.pcNVInsightsReportArchiveName : PcTestRunBuilder.pcReportArchiveName);
        try {
            restProxy.GetRunResultData(runId, result.getID(), reportArchiveFullPath);
        } catch (PcException ex)
        {
            if(!nvInsights)
                throw ex;
            else
                return null;
        }
        FilePath fp = new FilePath(new File(reportArchiveFullPath));
        fp.unzip(fp.getParent());
        fp.delete();
        FilePath reportFile = fp.sibling(nvInsights ? PcTestRunBuilder.pcNVInsightsReportFileName : PcTestRunBuilder.pcReportFileName);
        if (reportFile.exists()) {
            log(listener, (nvInsights ?  Messages.PublishingNVInsightsReport() : Messages.PublishingAnalysisReport()), true);
            return reportFile;
        }
        return null;
    }

    public boolean logout() {
        if (!loggedIn)
            return true;

        boolean logoutSucceeded = false;
        try {
            logoutSucceeded = restProxy.logout();
            loggedIn = !logoutSucceeded;
        } catch (PcException|IOException e) {
            log(listener, "%s: %s", true,
                    Messages.Error(),
                    e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
        }
        log(listener, "%s", true,
                logoutSucceeded ? Messages.LogoutSucceeded() : Messages.LogoutFailed());
        return logoutSucceeded;
    }

    public boolean stopRun(int runId) {
        boolean stopRunSucceeded = false;
        try {
            log(listener, "%s", true,
                    Messages.StoppingRun());
            stopRunSucceeded = restProxy.stopRun(runId, "stop");
        } catch (PcException|IOException e) {
            log(listener, "%s: %s", true,
                    Messages.Error(),
                    e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
        }
        log(listener, "%s", true,
                stopRunSucceeded ? Messages.StopRunSucceeded() : Messages.StopRunFailed());
        return stopRunSucceeded;
    }

    public PcRunEventLog getRunEventLog(int runId){
        try {
            return restProxy.getRunEventLog(runId);
        } catch (PcException|IOException e) {
            log(listener, "%s: %s", true,
                    Messages.Error(),
                    e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
        }
        return null;
    }

    public void addRunToTrendReport(int runId, String trendReportId)
    {

        TrendReportRequest trRequest = new TrendReportRequest(model.getAlmProject(true), runId, null);
        log(listener, "Adding run: %s to trend report: %s", true,
                runId,
                trendReportId);
        try {
            restProxy.updateTrendReport(trendReportId, trRequest);
            log(listener, "%s: %s %s: %s", true,
                    Messages.PublishingRun(),
                    runId,
                    Messages.OnTrendReport(),
                    trendReportId);
        } catch (PcException e) {
            log(listener, "%s: %s", true,
                    Messages.FailedToAddRunToTrendReport(),
                    e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
        } catch (IOException e) {
            log(listener, "%s: %s.", true,
                    Messages.FailedToAddRunToTrendReport(),
                    Messages.ProblemConnectingToPCServer());
            logStackTrace(listener, configureSystemSection, e);
        }
    }

    public void waitForRunToPublishOnTrendReport(int runId, String trendReportId) throws PcException,IOException,InterruptedException{

        ArrayList<PcTrendedRun> trendReportMetaDataResultsList;
        boolean publishEnded = false;
        int counterPublishStarted = 0;
        int counterPublishNotStarted = 0;
        boolean resultNotFound = true;

        do {
            trendReportMetaDataResultsList = restProxy.getTrendReportMetaData(trendReportId);

            if (trendReportMetaDataResultsList.isEmpty())  break;

            for (PcTrendedRun result : trendReportMetaDataResultsList) {
                resultNotFound = result.getRunID() != runId;
                if (resultNotFound) continue;

                if (result.getState().equals(PcTestRunBuilder.TRENDED) || result.getState().equals(PcTestRunBuilder.ERROR)){
                    publishEnded = true;
                    log(listener, "Run: %s %s: %s", true,
                            runId,
                            Messages.PublishingStatus(),
                            result.getState());
                    break;
                } else {
                    Thread.sleep(5000);
                    counterPublishStarted++;
                    if(counterPublishStarted >= 120){
                        String msg = String.format("%s: %s",
                                Messages.Error(),
                                Messages.PublishingEndTimeout());
                        throw new PcException(msg);
                    }
                }
            }
            if (!publishEnded && resultNotFound) {
                Thread.sleep(5000);
                counterPublishNotStarted++;
                if(counterPublishNotStarted >= 120){
                    String msg = String.format("%s: %s",
                            Messages.Error(),
                            Messages.PublishingStartTimeout());
                    throw new PcException(msg);
                }
            }
        } while (!publishEnded && counterPublishStarted < 120 && counterPublishNotStarted < 120);
    }

    public boolean downloadTrendReportAsPdf(String trendReportId, String directory) throws PcException {


        try {
            log(listener, "%s: %s %s", true,
                    Messages.DownloadingTrendReport(),
                    trendReportId,
                    Messages.InPDFFormat());
            InputStream in = restProxy.getTrendingPDF(trendReportId);
            File dir = new File(directory);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String filePath = directory + IOUtils.DIR_SEPARATOR + "trendReport" + trendReportId + ".pdf";
            Path destination = Paths.get(filePath);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log(listener, "%s: %s %s", true,
                    Messages.TrendReport(),
                    trendReportId,
                    Messages.SuccessfullyDownloaded());
        }
        catch (Exception e) {
            log(listener, "%s: %s", true,
                    Messages.FailedToDownloadTrendReport(),
                    e.getMessage());
            logStackTrace(listener, configureSystemSection, e);
            throw new PcException(e.getMessage());
        }

        return true;

    }

    public void publishTrendReport(String filePath, String trendReportId){

        if (filePath == null){return;}
        //     return String.format( HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));
        log(listener, "%s", false, HyperlinkNote.encodeTo(filePath, Messages.ViewTrendReport() + " " + trendReportId));

    }


    // This method will return a map with the following structure: <transaction_name:selected_measurement_value>
    // for example:
    // <Action_Transaction:0.001>
    // <Virtual transaction 2:0.51>
    // This function uses reflection since we know only at runtime which transactions data will be reposed from the rest request.
    public Map<String, String>  getTrendReportByXML(String trendReportId, int runId, TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType,TrendReportTypes.Measurement measurement) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        Map<String, String> measurmentsMap = new LinkedHashMap<String, String>();
        measurmentsMap.put("RunId","_" + runId + "_");
        measurmentsMap.put("Trend Measurement Type",measurement.toString() + "_" + pctType.toString());



        TrendReportTransactionDataRoot res = restProxy.getTrendReportByXML(trendReportId, runId);

//            java.lang.reflect.Method rootMethod =  res.getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowsList");
//            ArrayList<Object> RowsListObj = (ArrayList<Object>) rootMethod.invoke(res);
//            RowsListObj.get(0);

        List<Object> RowsListObj = res.getTrendReportRoot();

        for (int i=0; i< RowsListObj.size();i++){
            try {

                java.lang.reflect.Method rowListMethod = RowsListObj.get(i).getClass().getMethod("getTrendReport" + dataType.toString() + "DataRowList");

                for ( Object DataRowObj : (ArrayList<Object>)rowListMethod.invoke(RowsListObj.get(i)))
                {
                    if (DataRowObj.getClass().getMethod("getPCT_TYPE").invoke(DataRowObj).equals(pctType.toString()))
                    {
                        java.lang.reflect.Method method;
                        method = DataRowObj.getClass().getMethod("get" + measurement.toString());
                        measurmentsMap.put(DataRowObj.getClass().getMethod("getPCT_NAME").invoke(DataRowObj).toString(),method.invoke(DataRowObj)==null?"":method.invoke(DataRowObj).toString());
                    }
                }
            }catch (NoSuchMethodException e){
                //  logger.println("No such method exception: " + e);
                //logStackTrace(listener, configureSystemSection, e);
            }
            catch (Exception e){
                //log(listener, " Error on getTrendReportByXML: %s ", true, e.getMessage());
                //logStackTrace(listener, configureSystemSection, e);
            }
        }

        //  logger.print(res);

        return measurmentsMap;

    }

}
