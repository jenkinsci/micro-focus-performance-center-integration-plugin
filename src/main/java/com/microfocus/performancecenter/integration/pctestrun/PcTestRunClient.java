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

import hudson.FilePath;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

import hudson.console.HyperlinkNote;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

public class PcTestRunClient {

    private PcTestRunModel model;
    private PcRestProxy restProxy;
    private boolean loggedIn;
    public UsernamePasswordCredentials usernamePCPasswordCredentials;
    public UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy;
    TaskListener listener;

    public PcTestRunClient(PcTestRunModel pcTestRunModel, TaskListener listener) {
        try {
            this.listener = listener;
            model = pcTestRunModel;
            String credentialsProxyId = model.getCredentialsProxyId(true);
            usernamePCPasswordCredentialsForProxy = PcTestRunBuilder.getCredentialsId(credentialsProxyId);
            String proxyOutUser = (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getUsername();
            String proxyOutPassword= (usernamePCPasswordCredentialsForProxy == null || model.getProxyOutURL(true).isEmpty()) ? "" : usernamePCPasswordCredentialsForProxy.getPassword().getPlainText();
            if(model.getProxyOutURL(true) != null && !model.getProxyOutURL(true).isEmpty()) {
                log(listener, "Using proxy: %s", true,  model.getProxyOutURL(true));
                if(!proxyOutUser.isEmpty()) {
                    if (model.getCredentialsProxyId().startsWith("$"))
                        log(listener, "Using proxy credentials of %s as specified in build parameters.", true,  proxyOutUser);
                    else
                        log(listener, "Using proxy credentials of %s as specified in configuration.", true,  proxyOutUser);
                }
            }
            restProxy = new PcRestProxy(model.isHTTPSProtocol(),model.getPcServerName(true), model.getAlmDomain(true), model.getAlmProject(true), model.getProxyOutURL(true),proxyOutUser,proxyOutPassword);
        }catch (PcException e){
            log(listener, "Error: %s", true, e.getMessage());
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
            usernamePCPasswordCredentials = PcTestRunBuilder.getCredentialsId(credentialsId);
            if(usernamePCPasswordCredentials != null) {
                if(model.getCredentialsId().startsWith("$"))
                    log(listener, "Using Performance Center credentials supplied in build parameters", true);
                else
                    log(listener, "Using Performance Center credentials supplied in configuration", true);
                log(listener, "Trying to login\n[PCServer='%s://%s', User='%s']", true, model.isHTTPSProtocol(), model.getPcServerName(true), usernamePCPasswordCredentials.getUsername());
                loggedIn = restProxy.authenticate(usernamePCPasswordCredentials.getUsername(), usernamePCPasswordCredentials.getPassword().getPlainText());
            }
            else {
                log(listener, "Trying to login\n[PCServer='%s://%s', User='%s']", true, model.isHTTPSProtocol(), model.getPcServerName(true), PcTestRunBuilder.usernamePCPasswordCredentials.getUsername());
                loggedIn = restProxy.authenticate(PcTestRunBuilder.usernamePCPasswordCredentials.getUsername(), PcTestRunBuilder.usernamePCPasswordCredentials.getPassword().getPlainText());
            }
        } catch (PcException|IOException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }
        log(listener, "Login %s", true, loggedIn ? "succeeded" : "failed");
        return loggedIn;
    }

    public boolean isLoggedIn() {

        return loggedIn;
    }

    public int startRun() throws NumberFormatException, ClientProtocolException, PcException, IOException {




        int testID = Integer.parseInt(model.getTestId(true));
        int testInstance = getCorrectTestInstanceID(testID);
        setCorrectTrendReportID();

        log(listener, "Executing Load Test: \n====================\nTest ID: %s \nTest Instance ID: %s \nTimeslot Duration: %s \nPost Run Action: %s \nUse VUDS: %s\n====================\n", true, Integer.parseInt(model.getTestId(true)), testInstance, model.getTimeslotDuration() ,model.getPostRunAction().getValue(),model.isVudsMode());
        PcRunResponse response = null;
        try {
            response = restProxy.startRun(testID,
                    testInstance,
                    model.getTimeslotDuration(),
                    model.getPostRunAction().getValue(),
                    model.isVudsMode());
            log(listener, "Run started (TestID: %s, RunID: %s, TimeslotID: %s)", true, response.getTestID(), response.getID(), response.getTimeslotID());

            return response.getID();
        }
        catch (NumberFormatException|ClientProtocolException|PcException ex) {
            log(listener, "StartRun failed. Error: %s", true, ex.getMessage());
            logStackTrace(listener, ex);
        }
        catch (IOException ex) {
            log(listener, "StartRun failed. Error: %s", true, ex.getMessage());
            logStackTrace(listener, ex);
        }
        if (!("RETRY".equals(model.getRetry()))) {
            return 0;
        }
        else {
            //counter
            int retryCount = 0;
            //values
            int retryDelay = model.getRetryDelay();
            int retryOccurrences = model.getRetryOccurrences();

            while (retryCount<=retryOccurrences)
            {
                retryCount++;
                try {
                    if(retryCount <= retryOccurrences) {
                        log(listener, "Failed to start run. Attempting to start again in %s minute(s). %s attemp(s) remaining.", true, retryDelay, retryOccurrences - retryCount + 1);
                        Thread.sleep(retryDelay * 60 * 1000);
                    }
                }
                catch (InterruptedException ex) {
                    log(listener, "wait failed", true);
                    logStackTrace(listener, ex);
                }

                try {
                    response = restProxy.startRun(testID,
                            testInstance,
                            model.getTimeslotDuration(),
                            model.getPostRunAction().getValue(),
                            model.isVudsMode());
                }
                catch (NumberFormatException|ClientProtocolException|PcException ex) {
                    log(listener, "StartRun retry failed. Error: %s", true, ex.getMessage());
                    logStackTrace(listener, ex);
                } catch (IOException ex) {
                    log(listener, "StartRun retry failed. Error: %s", true, ex.getMessage());
                    logStackTrace(listener, ex);
                }
                int ret = 0;
                if (response !=null) {
                    try {
                        ret = response.getID();
                    }
                    catch (Exception ex) {
                        log(listener, "getID failed. Error: %s", true, ex.getMessage());
                        logStackTrace(listener, ex);
                    }
                }
                if (ret != 0) {
                    log(listener, "Run started (TestID: %s, RunID: %s, TimeslotID: %s)", true, response.getTestID(), response.getID(), response.getTimeslotID());
                    return ret;
                }
            }
        }
        return 0;
    }


    private int getCorrectTestInstanceID(int testID) throws IOException, PcException {
        if("AUTO".equals(model.getAutoTestInstanceID())){
            try {


                log(listener, "Searching for available test instance", true);
                PcTestInstances pcTestInstances = restProxy.getTestInstancesByTestId(testID);
                int testInstanceID;
                if (pcTestInstances != null && pcTestInstances.getTestInstancesList() != null){
                    PcTestInstance pcTestInstance = pcTestInstances.getTestInstancesList().get(pcTestInstances.getTestInstancesList().size()-1);
                    testInstanceID = pcTestInstance.getInstanceId();
                    log(listener, "Found test instance ID: %s", true, testInstanceID);
                }else{
                    log(listener, "Could not find existing test instanceID. Creating a new test instance.", true);
                    log(listener, "Searching for available TestSet", true);
                    // Get a random TestSet
                    PcTestSets pcTestSets = restProxy.GetAllTestSets();
                    if (pcTestSets !=null && pcTestSets.getPcTestSetsList() !=null){
                        PcTestSet pcTestSet = pcTestSets.getPcTestSetsList().get(pcTestSets.getPcTestSetsList().size()-1);
                        int testSetID = pcTestSet.getTestSetID();
                        log(listener, "Creating Test Instance with testID: %s and TestSetID: %s", true, testID,testSetID);
                        testInstanceID = restProxy.createTestInstance(testID,testSetID);
                        log(listener, "Test Instance with ID : %s has been created successfully.", true,testInstanceID);
                    }else{
                        String msg = "There is no TestSet available in the project. Please create a testset from Performance Center UI.";
                        log(listener, "Error: %s", true, msg);
                        throw new PcException(msg);
                    }
                }
                return testInstanceID;
            } catch (Exception e){
                log(listener, "getCorrectTestInstanceID failed, reason: %s", true, e.getMessage());
                logStackTrace(listener, e);
                return Integer.parseInt(null);
            }
        }
        return Integer.parseInt(model.getTestInstanceId(true));
    }

    private void setCorrectTrendReportID() throws IOException, PcException {
        // If the user selected "Use trend report associated with the test" we want the report ID to be the one from the test
        String msg = "No trend report ID is associated with the test.\n" +
                "Please turn Automatic Trending on for the test through Performance Center UI.\n" +
                "Alternatively you can check 'Add run to trend report with ID' on Jenkins job configuration.";
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
                    logStackTrace(listener, ex);
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
            logStackTrace(listener, ex);
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
                    log(listener, "Cannot get response from PC about the state of RunID: %s %s time(s) consecutively", true, runId, (3 - threeStrikes));
                    if(threeStrikes==0) {
                        log(listener, "stopping monitoring on RunID: %s", true, runId);
                        break;
                    }
                    Thread.sleep(2000);
                    login(listener);
                }
                response = restProxy.getRunData(runId);
                RunState currentState = RunState.get(response.getRunState());
                if (lastState.ordinal() < currentState.ordinal()) {
                    lastState = currentState;
                    log(listener, "RunID: %s - State = %s", true, runId, currentState.value());
                }

                // In case we are in state before collate or before analyze, we will wait 1 minute for the state to change otherwise we exit
                // because the user probably stopped the run from PC or timeslot has reached the end.
                if (Arrays.asList(states).contains(currentState)) {
                    counter++;
                    Thread.sleep(1000);
                    if (counter > 60) {
                        log(listener, "RunID: %s  - Stopped from Performance Center side with state = %s", true, runId, currentState.value());
                        break;
                    }
                } else {
                    counter = 0;
                    Thread.sleep(interval);
                }
                threeStrikes = 3;
            }
            catch(InterruptedException|PcException e)
            {
                threeStrikes--;
            }
        } while (lastState.ordinal() < completionState.ordinal());
        return response;
    }

    public FilePath publishRunReport(int runId, String reportDirectory) throws IOException, PcException, InterruptedException {
        PcRunResults runResultsList = restProxy.getRunResults(runId);
        if (runResultsList.getResultsList() != null){
            for (PcRunResult result : runResultsList.getResultsList()) {
                if (result.getName().equals(PcTestRunBuilder.pcReportArchiveName)) {
                    File dir = new File(reportDirectory);
                    dir.mkdirs();
                    String reportArchiveFullPath = dir.getCanonicalPath() + IOUtils.DIR_SEPARATOR + PcTestRunBuilder.pcReportArchiveName;
                    log(listener, "Publishing analysis report", true);
                    restProxy.GetRunResultData(runId, result.getID(), reportArchiveFullPath);
                    FilePath fp = new FilePath(new File(reportArchiveFullPath));
                    fp.unzip(fp.getParent());
                    fp.delete();
                    FilePath reportFile = fp.sibling(PcTestRunBuilder.pcReportFileName);
                    if (reportFile.exists())
                        return reportFile;
                }
            }
        }
        log(listener, "Failed to get run report", true);
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
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }
        log(listener, "Logout %s", true, logoutSucceeded ? "succeeded" : "failed");
        return logoutSucceeded;
    }

    public boolean stopRun(int runId) {
        boolean stopRunSucceeded = false;
        try {
            log(listener, "Stopping run", true);
            stopRunSucceeded = restProxy.stopRun(runId, "stop");
        } catch (PcException|IOException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }
        log(listener, "Stop run %s", true, stopRunSucceeded ? "succeeded" : "failed");
        return stopRunSucceeded;
    }

    public PcRunEventLog getRunEventLog(int runId){
        try {
            return restProxy.getRunEventLog(runId);
        } catch (PcException|IOException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }
        return null;
    }

    public void addRunToTrendReport(int runId, String trendReportId)
    {

        TrendReportRequest trRequest = new TrendReportRequest(model.getAlmProject(true), runId, null);
        log(listener, "Adding run: %s to trend report: %s", true, runId, trendReportId);
        try {
            restProxy.updateTrendReport(trendReportId, trRequest);
            log(listener, "Publishing run: %s on trend report: %s", true, runId, trendReportId);
        }
        catch (PcException e) {
            log(listener, "Failed to add run to trend report: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }
        catch (IOException e) {
            log(listener, "Failed to add run to trend report: Problem connecting to PC Server.", true);
            logStackTrace(listener, e);
        }
    }

    public void waitForRunToPublishOnTrendReport(int runId, String trendReportId) throws PcException,IOException,InterruptedException{

        ArrayList<PcTrendedRun> trendReportMetaDataResultsList;
        boolean publishEnded = false;
        int counter = 0;

        do {
            trendReportMetaDataResultsList = restProxy.getTrendReportMetaData(trendReportId);

            if (trendReportMetaDataResultsList.isEmpty())  break;

            for (PcTrendedRun result : trendReportMetaDataResultsList) {

                if (result.getRunID() != runId) continue;

                if (result.getState().equals(PcTestRunBuilder.TRENDED) || result.getState().equals(PcTestRunBuilder.ERROR)){
                    publishEnded = true;
                    log(listener, "Run: %s publishing status: %s.", true, runId, result.getState());
                    break;
                }else{
                    Thread.sleep(5000);
                    counter++;
                    if(counter >= 120){
                        String msg = "Error: Publishing didn't ended after 10 minutes, aborting...";
                        throw new PcException(msg);
                    }
                }
            }

        }while (!publishEnded && counter < 120);
    }

    public boolean downloadTrendReportAsPdf(String trendReportId, String directory) throws PcException {


        try {
            log(listener, "Downloading trend report: %s in PDF format", true, trendReportId);
            InputStream in = restProxy.getTrendingPDF(trendReportId);
            File dir = new File(directory);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String filePath = directory + IOUtils.DIR_SEPARATOR + "trendReport" + trendReportId + ".pdf";
            Path destination = Paths.get(filePath);
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            log(listener, "Trend report: %s was successfully downloaded", true, trendReportId);
        }
        catch (Exception e) {
            log(listener, "Failed to download trend report: %s", true, e.getMessage());
            logStackTrace(listener, e);
            throw new PcException(e.getMessage());
        }

        return true;

    }

    public void publishTrendReport(String filePath, String trendReportId){

        if (filePath == null){return;}
        //     return String.format( HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));
        log(listener, "%s", false, HyperlinkNote.encodeTo(filePath, "View trend report " + trendReportId));

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
                //logStackTrace(listener, e);
            }
            catch (Exception e){
                //log(listener, " Error on getTrendReportByXML: %s ", true, e.getMessage());
                //logStackTrace(listener, e);
            }
        }




        //  logger.print(res);


        return measurmentsMap;


    }

}
