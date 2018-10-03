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
 * Create the PCModel and the PCClient and allows the connection between the job and PC
 * */
package com.microfocus.performancecenter.integration.pctestrun;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.*;
import com.microfocus.performancecenter.integration.pctestrun.helper.AdditionalParametersAction;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.Error;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.Failure ;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.JUnitTestCaseStatus ;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.Testcase ;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.Testsuite ;
import com.microfocus.performancecenter.integration.common.helpers.result.model.junit.Testsuites ;
import com.microfocus.performancecenter.integration.common.helpers.configuration.ConfigurationService ;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

import hudson.Util;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.PluginWrapper;
import hudson.console.HyperlinkNote;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.beans.IntrospectionException;
import java.io.*;
import java.lang.reflect.Method;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.FINISHED;
import static com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState.RUN_FAILURE;

public class PcTestRunBuilder extends Builder implements SimpleBuildStep{

    private static final String artifactsDirectoryName = "archive";
    public static final String artifactsResourceName = "artifact";
    public static final String runReportStructure = "%s/%s/performanceTestsReports/pcRun";
    public static final String trendReportStructure = "%s/%s/performanceTestsReports/TrendReports";
    public static final String pcReportArchiveName = "Reports.zip";
    public static final String pcReportFileName = "Report.html";
    private static final String RUNID_BUILD_VARIABLE = "HP_RUN_ID";

    public static final String    TRENDED         = "Trended";
    public static final String    PENDING         = "Pending";
    public static final String    PUBLISHING      = "Publishing";
    public static final String    ERROR           = "Error";

    private PcTestRunModel pcTestRunModel;
    public static UsernamePasswordCredentials usernamePCPasswordCredentials;
    public static UsernamePasswordCredentials usernamePCPasswordCredentialsForProxy;
    private transient static Run<?, ?> _run;

    private final String timeslotDurationHours;
    private final String timeslotDurationMinutes;
    private final boolean statusBySLA;

    private String serverAndPort;
    private String pcServerName;
    private String credentialsId;
    private String almDomain;
    private String almProject;
    private String testId;
    private String testInstanceId;
    private String autoTestInstanceID;
    private PostRunAction postRunAction;
    private boolean vudsMode;
    private String description;
    private String addRunToTrendReport;
    private String trendReportId;
    private boolean HTTPSProtocol;
    private String proxyOutURL;
    private String credentialsProxyId;
    private String retry;
    private String retryDelay;
    private String retryOccurrences;

    private int runId;
    private String testName;
    private FilePath pcReportFile;
    private String junitResultsFileName;
    private static PrintStream logger;
    private File WorkspacePath;
    private transient AbstractBuild<?, ?> _build;
    private TaskListener listener;

    @DataBoundConstructor
    public PcTestRunBuilder(
            String serverAndPort,
            String pcServerName,
            String credentialsId,
            String almDomain,
            String almProject,
            String testId,
            String testInstanceId,
            String autoTestInstanceID,
            String timeslotDurationHours,
            String timeslotDurationMinutes,
            PostRunAction postRunAction,
            boolean vudsMode,
            boolean statusBySLA,
            String description,
            String addRunToTrendReport,
            String trendReportId,
            boolean HTTPSProtocol,
            String proxyOutURL,
            String credentialsProxyId,
            String retry,
            String retryDelay,
            String retryOccurrences) {

        this.serverAndPort = serverAndPort;
        this.pcServerName = pcServerName;
        this.credentialsId = credentialsId;
        this.almDomain = almDomain;
        this.almProject = almProject;
        this.testId = testId;
        this.testInstanceId = testInstanceId;
        this.autoTestInstanceID = autoTestInstanceID;
        this.timeslotDurationHours = timeslotDurationHours;
        this.timeslotDurationMinutes = timeslotDurationMinutes;
        this.postRunAction = postRunAction;
        this.vudsMode = vudsMode;
        this.statusBySLA = statusBySLA;
        this.description = description;
        this.addRunToTrendReport = addRunToTrendReport;
        this.trendReportId = trendReportId;
        this.HTTPSProtocol = HTTPSProtocol;
        this.proxyOutURL = proxyOutURL;
        this.credentialsProxyId  = credentialsProxyId;
        this.retry = (retry == null || retry.isEmpty())? "NO_RETRY" : retry;
        this.retryDelay = ("NO_RETRY".equals(this.retry)) ? "0" : (retryDelay == null || retryDelay.isEmpty()) ? "5" : retryDelay;
        this.retryOccurrences = ("NO_RETRY".equals(this.retry)) ? "0" : (retryOccurrences == null || retryOccurrences.isEmpty()) ? "3" : retryOccurrences;
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        _build = build;
        if(build.getWorkspace() != null)
            WorkspacePath =  new File(build.getWorkspace().toURI());
        else
            WorkspacePath =  null;
        if((getPcTestRunModel() != null) && (build != null) && (build instanceof AbstractBuild))
            setPcTestRunModelBuildParameters(build);
        if(build.getWorkspace() != null)
            perform(build, build.getWorkspace(), launcher, listener);
        else
            return false;
        return true;
    }

    private void setPcTestRunModelBuildParameters(AbstractBuild<?, ?> build) {
        String buildParameters = build.getBuildVariables().toString();
        if (!buildParameters.isEmpty())
            getPcTestRunModel().setBuildParameters(buildParameters);
    }

    public File getWorkspacePath(){
        return WorkspacePath;
    }


    public String getCredentialsId() {
        return credentialsId;
    }

    public String getCredentialsProxyId() {
        return credentialsProxyId;
    }

    public static UsernamePasswordCredentials getCredentialsId(String credentialsId)
    {
        if(credentialsId!=null && _run != null )
            return getCredentialsById(credentialsId, _run, logger);
        return null;
    }

    public static UsernamePasswordCredentials getCredentialsProxyId(String credentialsProxyId)
    {
        if(credentialsProxyId!=null && _run != null )
            return getCredentialsById(credentialsProxyId, _run, logger);
        return null;
    }


    public  void setCredentialsId(String newCredentialsId)
    {
        credentialsId = newCredentialsId;
        pcTestRunModel = null;
        getPcTestRunModel();
    }

    public  void setCredentialsProxyId(String newCredentialsProxyId)
    {
        credentialsProxyId = newCredentialsProxyId;
        pcTestRunModel = null;
        getPcTestRunModel();
    }

    private static UsernamePasswordCredentials getCredentialsById(String credentialsId, Run<?, ?> run, PrintStream logger) {
        if (StringUtils.isBlank(credentialsId))
            return null;

        UsernamePasswordCredentials usernamePCPasswordCredentials = CredentialsProvider.findCredentialById(credentialsId,
                StandardUsernamePasswordCredentials.class,
                run,
                URIRequirementBuilder.create().build());

        if (usernamePCPasswordCredentials == null) {
            logger.println("Cannot find credentials with the credentialsId:" + credentialsId);
        }

        return usernamePCPasswordCredentials;
    }

    //pcTestRunModel is intialized here.
    public PcTestRunModel getPcTestRunModel() {
        if(pcTestRunModel == null)
        {
            pcTestRunModel =
                    new PcTestRunModel(
                            serverAndPort.trim(),
                            pcServerName.trim(),
                            credentialsId,
                            almDomain.trim(),
                            almProject.trim(),
                            testId.trim(),
                            autoTestInstanceID,
                            testInstanceId.trim(),
                            timeslotDurationHours.trim(),
                            timeslotDurationMinutes.trim(),
                            postRunAction,
                            vudsMode,
                            description,
                            addRunToTrendReport,
                            trendReportId,
                            HTTPSProtocol,
                            proxyOutURL,
                            credentialsProxyId,
                            retry,
                            retryDelay,
                            retryOccurrences);
        }
        return pcTestRunModel;
    }

    public String getRunResultsFileName() {

        return junitResultsFileName;
    }

    public static String getArtifactsDirectoryName() {

        return artifactsDirectoryName;
    }

    public static String getArtifactsResourceName() {

        return artifactsResourceName;
    }

    public static String getRunReportStructure() {

        return runReportStructure;
    }

    public static String getPcReportArchiveName() {

        return pcReportArchiveName;
    }

    public static String getPcreportFileName() {

        return pcReportFileName;
    }

    private void setBuildParameters (AbstractBuild<?, ?> build)
    {
        try {
            if (build != null && build.getBuildVariables() != null)
                getPcTestRunModel().setBuildParameters(build.getBuildVariables().toString());
        }
        catch (Exception ex) {
            logger.println(String.format("Build parameters will not be taken in consideration: %s", ex.getMessage()));
        }
    }


    private Testsuites execute(TaskListener listener, PcTestRunClient pcTestRunClient, Run<?, ?> build)
            throws InterruptedException,NullPointerException {
        _run = build;
        this.listener = listener;
        try {
            try {
                String version = ConfigurationService.getVersion();
                if(!(version == null || version.equals("unknown")))
                    log(listener, "plugin version is '%s'", true, version);
            } catch(IllegalStateException ex) {
                log(listener, "Error: IllegalStateException '%s'", true, ex.getMessage());
            }
            if((getPcTestRunModel() !=null) && (build != null) && (build instanceof AbstractBuild))
                setPcTestRunModelBuildParameters((AbstractBuild) build);
            if (!StringUtils.isBlank(getPcTestRunModel().getDescription()))
                log(listener, "Test description: %s", true, getPcTestRunModel().getDescription());
            if (!beforeRun(pcTestRunClient, listener))
                return null;

            return run(pcTestRunClient, build, listener);

        } catch (InterruptedException e) {
            build.setResult(Result.ABORTED);
            pcTestRunClient.stopRun(runId);
            throw e;
        } catch (NullPointerException e) {
            log(listener, "Error: %s", true, e.getMessage());
        } catch (Exception e) {
            log(listener, "Error: %s", true, e.getMessage());
        } finally {
            pcTestRunClient.logout();
        }
        return null;
    }

    private Testsuites run(PcTestRunClient pcTestRunClient, Run<?, ?> build, TaskListener listener)
            throws InterruptedException, ClientProtocolException,
            IOException, PcException {
        if((getPcTestRunModel() !=null) && (build != null) && (build instanceof AbstractBuild))
            setPcTestRunModelBuildParameters((AbstractBuild) build);
        PcRunResponse response = null;
        String errorMessage = "";
        String eventLogString = "";
        boolean trendReportReady = false;
        try {
            runId = pcTestRunClient.startRun();
            if (runId == 0)
                return null;
        }
        catch (NumberFormatException|PcException|IOException ex) {
            log(listener, "startRun failed. Error: %s", true, ex.getMessage());
            logStackTrace(listener, ex);
            throw ex;
        }

        //getTestName failure should not fail test execution.
        try {
            testName = pcTestRunClient.getTestName();
            if(testName == null) {
                testName = String.format("TestId_%s", getPcTestRunModel().getTestId());
                log(listener, "getTestName failed. Using '%s' as testname.", true, testName);
            }
            else
                log(listener, "test name is %s", true, testName);
        }
        catch (PcException|IOException ex) {
            testName = String.format("TestId_%s", getPcTestRunModel().getTestId());
            log(listener, "getTestName failed. Using '%s' as testname. Error: %s \n", true, testName, ex.getMessage());
            logStackTrace(listener, ex);
        }


        try {
            List<ParameterValue> parameters = new ArrayList<>();
            parameters.add(new StringParameterValue(RUNID_BUILD_VARIABLE, "" + runId));
            // This allows a user to access the runId from within Jenkins using a build variable.
            build.addAction(new AdditionalParametersAction(parameters));
            log(listener, "Set %s Environment Variable to %s \n", true, RUNID_BUILD_VARIABLE, runId);
            response = pcTestRunClient.waitForRunCompletion(runId);

            if (response != null && RunState.get(response.getRunState()) == FINISHED && getPcTestRunModel().getPostRunAction() != PostRunAction.DO_NOTHING) {
                pcReportFile = pcTestRunClient.publishRunReport(runId, getReportDirectory(build));

                // Adding the trend report section if ID has been set or if the Associated Trend report is selected.
                if(((("USE_ID").equals(getPcTestRunModel().getAddRunToTrendReport()) && getPcTestRunModel().getTrendReportId(true) != null) || ("ASSOCIATED").equals(getPcTestRunModel().getAddRunToTrendReport())) && RunState.get(response.getRunState()) != RUN_FAILURE){
                    Thread.sleep(5000);
                    pcTestRunClient.addRunToTrendReport(this.runId, getPcTestRunModel().getTrendReportId(true));
                    pcTestRunClient.waitForRunToPublishOnTrendReport(this.runId, getPcTestRunModel().getTrendReportId(true));
                    pcTestRunClient.downloadTrendReportAsPdf(getPcTestRunModel().getTrendReportId(true), getTrendReportsDirectory(build));
                    trendReportReady = true;
                }

            } else if (response != null && RunState.get(response.getRunState()).ordinal() > FINISHED.ordinal()) {
                PcRunEventLog eventLog = pcTestRunClient.getRunEventLog(runId);
                eventLogString = buildEventLogString(eventLog);
            }

        } catch (PcException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
        }

        Testsuites ret = new Testsuites();
        parsePcRunResponse(ret,response, build, errorMessage, eventLogString);
        try {
            parsePcTrendResponse(ret,build, pcTestRunClient,trendReportReady, getPcTestRunModel().getTrendReportId(true),runId);
        } catch (IntrospectionException|NoSuchMethodException e) {
            logStackTrace(listener, e);
        }

        return ret;
    }

    private String buildEventLogString(PcRunEventLog eventLog) {

        String logFormat = "%-5s | %-7s | %-19s | %s\n";
        StringBuilder eventLogStr = new StringBuilder("Event Log:\n\n" + String.format(logFormat, "ID", "TYPE", "TIME","DESCRIPTION"));
        for (PcRunEventLogRecord record : eventLog.getRecordsList()) {
            eventLogStr.append(String.format(logFormat, record.getID(), record.getType(), record.getTime(), record.getDescription()));
        }
        return eventLogStr.toString();
    }

    private boolean beforeRun(PcTestRunClient pcTestRunClient, TaskListener listener) {
        return validatePcForm(listener) && pcTestRunClient.login(listener);
    }

    private String getReportDirectory(Run<?, ?> build) {
        return String.format(
                runReportStructure,
                build.getRootDir().getPath(),
                artifactsDirectoryName);
    }

    private String getTrendReportsDirectory(Run<?, ?> build) {
        return String.format(
                trendReportStructure,
                build.getRootDir().getPath(),
                artifactsDirectoryName);
    }


    @Override
    @Deprecated
    public boolean perform(Build<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return super.perform(build, launcher, listener);
    }

    private boolean validatePcForm(TaskListener listener) {

        log(listener, "Validating parameters before run", true);
        String prefix = "doCheck";
        boolean ret = true;
        Method[] methods = getDescriptor().getClass().getMethods();
        Method[] modelMethods = getPcTestRunModel().getClass().getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith(prefix)) {
                name = name.replace(prefix, "").toLowerCase();
                for (Method modelMethod : modelMethods) {
                    String modelMethodName = modelMethod.getName();
                    if (modelMethodName.toLowerCase().equals("get" + name) && modelMethod.getParameterTypes().length==0) {
                        try {
                            Object obj = FormValidation.ok();
                            if (!("testinstanceid".equals(name) && "AUTO".equals(getPcTestRunModel().getAutoTestInstanceID()))
                                    && !(("retrydelay".equals(name) && "NO_RETRY".equals(getPcTestRunModel().getRetry())) || getPcTestRunModel().getRetry().isEmpty())
                                    && !(("retryoccurrences".equals(name) && "NO_RETRY".equals(getPcTestRunModel().getRetry())) || getPcTestRunModel().getRetry().isEmpty())
                                    ) {
                                if("doCheckCredentialsId".equals(method.getName()) && "credentialsid".equals(name) && "getCredentialsId".equals(modelMethodName)
                                        || "doCheckCredentialsProxyId".equals(method.getName()) && "credentialsproxyid".equals(name) && "getCredentialsProxyId".equals(modelMethodName)
                                        )
                                    obj = method.invoke(getDescriptor(), null,null, modelMethod.invoke(getPcTestRunModel()));
                                else
                                    obj = method.invoke(getDescriptor(), modelMethod.invoke(getPcTestRunModel()));
                            }
                            if (!obj.equals(FormValidation.ok())) {
                                log(listener, obj.toString(), false);
                                ret = false;
                            }
                            break;
                        } catch (Exception e) {
                            log(listener, "Validation error: method.getName() = '%s', name = '%s', modelMethodName = '%s', exception = '%s'.", true);
                            logStackTrace(listener, e);
                        }
                    }
                }
            }
        }

        boolean isTrendReportIdValid = validateTrendReportIdIsNumeric(getPcTestRunModel().getTrendReportId(true),("USE_ID").equals(getPcTestRunModel().getAddRunToTrendReport()));

        ret &= isTrendReportIdValid;
        return ret;

    }



    private boolean validateTrendReportIdIsNumeric(String trendReportId, boolean addRunToTrendReport){

        FormValidation res = FormValidation.ok();
        if(addRunToTrendReport){
            if(trendReportId.isEmpty()){
                res = FormValidation.error("Parameter Is Missing: trend report ID is missing");
            }
            else{

                try{

                    Integer.parseInt(trendReportId);
                }
                catch(NumberFormatException e) {

                    res = FormValidation.error("Illegal Parameter: trend report ID is not a number");
                }

            }
        }

        logger.println(String.format("%s", res.toString().replace(": <div/>","")));

        return res.equals(FormValidation.ok());
    }

    private Testsuites parsePcRunResponse(Testsuites ret,
                                          PcRunResponse runResponse,
                                          Run<?, ?> build,
                                          String errorMessage, String eventLogString) throws IOException, InterruptedException {

        RunState runState = RunState.get(runResponse.getRunState());


        List<Testsuite> testSuites = ret.getTestsuite();
        Testsuite testSuite = new Testsuite();
        Testcase testCase = new Testcase();
        //testCase.setClassname("Performance Tests.Test ID: " + runResponse.getTestID());
        testCase.setClassname("Performance Test.Load Test");
        testCase.setName(testName + "(ID:" + runResponse.getTestID() + ")");
        testCase.setTime(String.valueOf(runResponse.getDuration() * 60));
        if (pcReportFile != null && pcReportFile.exists() && runState == FINISHED) {
            testCase.getSystemOut().add(getOutputForReportLinks(build));
        }
        updateTestStatus(testCase, runResponse, errorMessage, eventLogString);
        testSuite.getTestcase().add(testCase);
        testSuite.setName("Performance Test ID: " + runResponse.getTestID() + ", Run ID: " + runResponse.getID());
        testSuites.add(testSuite);
        return ret;
    }

    private Testsuites parsePcTrendResponse(Testsuites ret, Run<?, ?> build, PcTestRunClient pcTestRunClient, boolean trendReportReady, String TrendReportID, int runID) throws PcException,IntrospectionException,IOException, InterruptedException ,NoSuchMethodException{


        if(trendReportReady){
            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, getPcTestRunModel().getTrendReportId(true));
            pcTestRunClient.publishTrendReport(reportUrl, getPcTestRunModel().getTrendReportId(true));

            // Updating all CSV files for plot plugin
            // this helps to show the transaction of each result
            if (isPluginActive("Plot plugin")) {
                logger.println(String.format("Updating csv files for Trending Charts."));
                updateCSVFilesForPlot(pcTestRunClient, runID);
                String plotUrlPath = "/job/" + build.getParent().getName() + "/plot";
                logger.println(String.format("%s", HyperlinkNote.encodeTo(plotUrlPath, "Trending Charts"))); // + HyperlinkNote.encodeTo("https://wiki.jenkins-ci.org/display/JENKINS/HP+Application+Automation+Tools#HPApplicationAutomationTools-RunningPerformanceTestsusingHPPerformanceCenter","More Info"));
            }else{
                logger.println(String.format("You can view Trending Charts directly from Jenkins using Plot Plugin, see more details on the %s (Performance Center 12.55 and Later).", HyperlinkNote.encodeTo("https://wiki.jenkins.io/display/JENKINS/MICRO+FOCUS+Application+Automation+Tools#MicroFocusApplicationAutomationTools-RunningPerformanceTestsusingPerformanceCenter","documentation")));
            }
        }
        return ret;
    }

    private boolean isPluginActive(String pluginDisplayName){
        List<PluginWrapper> allPlugin = Jenkins.getInstance().pluginManager.getPlugins();
        for (PluginWrapper pw :
                allPlugin) {

            if (pw.getDisplayName().toLowerCase().equals(pluginDisplayName.toLowerCase())) {
                return pw.isActive();
            }
        }
        return false;
    }

    private class TriTrendReportTypes {

        private TrendReportTypes.DataType dataType;
        private TrendReportTypes.PctType pctType;
        private TrendReportTypes.Measurement measurement;

        public TrendReportTypes.DataType getDataType() {
            return dataType;
        }

        public TrendReportTypes.PctType getPctType() {
            return pctType;
        }

        public TrendReportTypes.Measurement getMeasurement() {
            return measurement;
        }

        TriTrendReportTypes (TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement) {
            this.dataType = dataType;
            this.pctType = pctType;
            this.measurement = measurement;
        }
    }

    private void updateCSVFilesForPlot(PcTestRunClient pcTestRunClient, int runId) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        TriTrendReportTypes triTrendReportTypes[] = {
                // Transaction - TRT
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_STDDEVIATION),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_COUNT1),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_PERCENTILE_90),
                // Transaction - TPS
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_SUM1),
                // Transaction - TRS
                new TriTrendReportTypes(TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_COUNT1),
                // Monitors - UDP
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_STDDEVIATION),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_COUNT1),
                new TriTrendReportTypes(TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_SUM1),
                // Regular - VU
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_AVERAGE),
                // Regular - WEB
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MINIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MAXIMUM),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_AVERAGE),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MEDIAN),
                new TriTrendReportTypes(TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_SUM1)
        };

        try {
            for (TriTrendReportTypes triTrendReportType : triTrendReportTypes
                    ) {
                saveFileToWorkspacePath(pcTestRunClient, getPcTestRunModel().getTrendReportId(true), runId, triTrendReportType.getDataType(), triTrendReportType.getPctType(), triTrendReportType.getMeasurement());
            }
        } catch (PcException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
            updateCSVFilesForPlotFailover(pcTestRunClient, runId);
            return;
        } catch (Exception e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
            updateCSVFilesForPlotFailover(pcTestRunClient, runId);
            return;
        }

    }

    private void updateCSVFilesForPlotFailover(PcTestRunClient pcTestRunClient, int runId) throws IOException, PcException, IntrospectionException, NoSuchMethodException {

        //Map<String, String> measurementMap =pcClient.getTrendReportByXML(getPcModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE);

        // Transaction - TRT
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_MEDIAN);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_COUNT1);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_SUM1);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRT, TrendReportTypes.Measurement.PCT_PERCENTILE_90);

        // Transaction - TPS
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcTestRunClient,getPcModel().getTrendReportId(),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TPS, TrendReportTypes.Measurement.PCT_SUM1);

        // Transaction - TRS
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MINIMUM);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MAXIMUM);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_AVERAGE);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Transaction, TrendReportTypes.PctType.TRS, TrendReportTypes.Measurement.PCT_COUNT1);


        // Monitors - UDP
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_MEDIAN);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_COUNT1);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Monitors, TrendReportTypes.PctType.UDP, TrendReportTypes.Measurement.PCT_SUM1);

        // Regular - VU
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_AVERAGE);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.VU, TrendReportTypes.Measurement.PCT_STDDEVIATION);


        // Regular - WEB
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MINIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MAXIMUM);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_AVERAGE);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_MEDIAN);
        //saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_STDDEVIATION);
        saveFileToWorkspacePath(pcTestRunClient,getPcTestRunModel().getTrendReportId(true),runId,TrendReportTypes.DataType.Regular, TrendReportTypes.PctType.WEB, TrendReportTypes.Measurement.PCT_SUM1);

        //  logger.print(build.getRootDir().getPath());
    }

    private boolean saveFileToWorkspacePath(PcTestRunClient pcTestRunClient, String trendReportID, int runId, TrendReportTypes.DataType dataType, TrendReportTypes.PctType pctType, TrendReportTypes.Measurement measurement)throws IOException, PcException, IntrospectionException, NoSuchMethodException{
        String fileName = measurement.toString().toLowerCase()  + "_" +  pctType.toString().toLowerCase() + ".csv";
        Map<String, String> measurementMap;
        try {
            measurementMap = pcTestRunClient.getTrendReportByXML(trendReportID, runId, dataType, pctType, measurement);
        } catch (PcException e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
            throw e;
        } catch (Exception e) {
            log(listener, "Error: %s", true, e.getMessage());
            logStackTrace(listener, e);
            throw e;
        }

        if (!_build.getWorkspace().isRemote()) {
            try {
                File file = new File(getWorkspacePath().getPath() + "/" + fileName);

                if (!file.exists()) {
                    file.createNewFile();
                }
                PrintWriter writer = new PrintWriter(file);
                for (String key : measurementMap.keySet()) {
                    writer.print(key + ",");
                }
                writer.print("\r\n");
                for (String value : measurementMap.values()) {
                    writer.print(value + ",");
                }
                writer.close();
                return true;
            } catch (IOException e) {
                if (getWorkspacePath().getPath() != null) {
                    log(listener, "Error saving file: %s to workspace path: %s with Error: %s", true,getWorkspacePath().getPath(), fileName, e.getMessage());
                    logStackTrace(listener, e);
                }
                else {
                    log(listener, "Error saving file: %s because workspace path is unavailable. Error: %s", true, fileName, e.getMessage());
                    logStackTrace(listener, e);
                }
                throw e;
            }
        }
        else {
            try {
                FilePath filePath = new FilePath(_build.getWorkspace().getChannel(), getWorkspacePath().getPath() + "/" + fileName);
                String filepathContent="";
                for (String key : measurementMap.keySet()) {
                    filepathContent += key + ",";
                }
                filepathContent += "\r\n";
                for (String value : measurementMap.values()) {
                    filepathContent += value + ",";
                }
                filePath.write(filepathContent, null);
                return true;
            } catch (InterruptedException e) {
                if (getWorkspacePath().getPath() != null) {
                    log(listener, "Error saving file: %s to remote workspace path: %s with Error: %s", true, getWorkspacePath().getPath(), fileName, e.getMessage());
                    logStackTrace(listener, e);
                } else {
                    log(listener, "Error saving file: %s because remote workspace path is unavailable. Error: %s", true, fileName, e.getMessage());
                    logStackTrace(listener, e);
                }
                return false;
            }
        }

    }



    private void updateTestStatus(Testcase testCase, PcRunResponse response, String errorMessage, String eventLog) {
        RunState runState = RunState.get(response.getRunState());
        if (runState == RUN_FAILURE) {
            setError(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else if (statusBySLA && runState == FINISHED && !(response.getRunSLAStatus().equalsIgnoreCase("passed"))) {
            setFailure(testCase, "Run measurements did not reach SLA criteria. Run SLA Status: "
                    + response.getRunSLAStatus(), eventLog);
        } else if (runState.hasFailure()) {
            setFailure(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        } else if(errorMessage != null && !errorMessage.isEmpty()){
            setFailure(testCase, String.format("%s. %s", runState, errorMessage), eventLog);
        }
        else{
            testCase.setStatus(JUnitTestCaseStatus.PASS);
        }
    }

    private void setError(Testcase testCase, String message, String eventLog) {
        Error error = new Error();
        error.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getError().add(error);
        testCase.setStatus(JUnitTestCaseStatus.ERROR);
        logger.println(String.format("%s %s", message ,eventLog));
    }

    private void setFailure(Testcase testCase, String message, String eventLog) {
        Failure failure = new Failure();
        failure.setMessage(message);
        if (!(eventLog == null || eventLog.isEmpty()))
            testCase.getSystemErr().add(eventLog);
        testCase.getFailure().add(failure);
        testCase.setStatus(JUnitTestCaseStatus.FAILURE);
        logger.println(String.format("Failure: %s %s", message ,eventLog));
    }

    private String getOutputForReportLinks(Run<?, ?> build) {
        String urlPattern = getArtifactsUrlPattern(build);
        String viewUrl = String.format(urlPattern + "/%s", pcReportFileName);
        String downloadUrl = String.format(urlPattern + "/%s", "*zip*/pcRun");
        logger.println(String.format("%s", HyperlinkNote.encodeTo(viewUrl, "View analysis report of run " + runId)));

        return String.format("Load Test Run ID: %s\n\nView analysis report:\n%s\n\nDownload Report:\n%s", runId, getPcTestRunModel().getserverAndPort() +  "/" +  build.getUrl() + viewUrl, getPcTestRunModel().getserverAndPort() + "/" + build.getUrl() + downloadUrl);
    }

    private String getArtifactsUrlPattern(Run<?, ?> build) {

        String runReportUrlTemp = runReportStructure.replaceFirst("%s/", "");
        return String.format(
                runReportUrlTemp,
                artifactsResourceName);
    }

    private void provideStepResultStatus(Result resultStatus, Run<?, ?> build) {
        String runIdStr =
                (runId > 0) ? String.format(" (PC RunID: %s)", String.valueOf(runId)) : "";
        logger.println(String.format("Result Status%s: %s\n- - -",
                runIdStr,
                resultStatus.toString()));
        build.setResult(resultStatus);

    }

    private Result createRunResults(FilePath filePath, Testsuites testsuites) {
        Result ret = Result.SUCCESS;
        try {
            if (testsuites != null) {
                StringWriter writer = new StringWriter();
                JAXBContext context = JAXBContext.newInstance(Testsuites.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.marshal(testsuites, writer);
                filePath.write(writer.toString(), null);
                if (containsErrorsOrFailures(testsuites.getTestsuite())) {
                    ret = Result.FAILURE;
                }
            } else {
                logger.println(String.format("Empty Results"));
                ret = Result.FAILURE;
            }

        } catch (Exception cause) {
            logger.print(String.format(
                    "Failed to create run results, Exception: %s",
                    cause.getMessage()));
            ret = Result.FAILURE;
        }
        return ret;
    }

    private boolean containsErrorsOrFailures(List<Testsuite> testsuites) {
        boolean ret = false;
        for (Testsuite testsuite : testsuites) {
            for (Testcase testcase : testsuite.getTestcase()) {
                String status = testcase.getStatus();
                if (status.equals(JUnitTestCaseStatus.ERROR)
                        || status.equals(JUnitTestCaseStatus.FAILURE)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    private String getJunitResultsFileName() {
        Format formatter = new SimpleDateFormat("ddMMyyyyHHmmssSSS");
        String time = formatter.format(new Date());
        junitResultsFileName = String.format("Results%s.xml", time);
        return junitResultsFileName;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        this.listener = listener;
        Result resultStatus = Result.FAILURE;
        //trendReportReady = false;
        logger = listener.getLogger();
        if(credentialsId != null)
            usernamePCPasswordCredentials = getCredentialsById(credentialsId, build, logger);
        if(credentialsProxyId != null && !credentialsProxyId.isEmpty())
            usernamePCPasswordCredentialsForProxy = getCredentialsById(credentialsProxyId, build, logger);
        PcTestRunClient pcTestRunClient = new PcTestRunClient(getPcTestRunModel(), listener);
        Testsuites testsuites = execute(listener, pcTestRunClient, build);

//        // Create Trend Report
//        if(trendReportReady){
//            String reportUrlTemp = trendReportStructure.replaceFirst("%s/", "") + "/trendReport%s.pdf";
//            String reportUrl = String.format(reportUrlTemp, artifactsResourceName, getPcTestRunModel().getTrendReportId(true));
//            pcTestRunClient.publishTrendReport(reportUrl, getPcTestRunModel().getTrendReportId(true));
//        }
//        // End Create Trend Report

        FilePath resultsFilePath = workspace.child(getJunitResultsFileName());
        resultStatus = createRunResults(resultsFilePath, testsuites);
        provideStepResultStatus(resultStatus, build);

        if (!Result.SUCCESS.equals(resultStatus) && !Result.FAILURE.equals(resultStatus)) {
            return;
        }
//        //Only do this if build worked (Not unstable or aborted - which might mean there is no report
//        JUnitResultArchiver jUnitResultArchiver = new JUnitResultArchiver(this.getRunResultsFileName());
//        jUnitResultArchiver.setKeepLongStdio(true);
//        jUnitResultArchiver.perform(build, workspace, launcher, listener);

    }

    public String getServerAndPort()
    {
        return getPcTestRunModel().getserverAndPort();
    }
    public String getPcServerName()
    {
        return getPcTestRunModel().getPcServerName();
    }

    public String getAlmProject()
    {
        return getPcTestRunModel().getAlmProject();
    }
    public String getTestId()
    {
        return getPcTestRunModel().getTestId();
    }
    public String getAlmDomain()
    {
        return getPcTestRunModel().getAlmDomain();
    }
    public String getTimeslotDurationHours()
    {
        return timeslotDurationHours;
    }
    public String getTimeslotDurationMinutes()
    {
        return timeslotDurationMinutes;
    }
    public PostRunAction getPostRunAction()
    {
        return getPcTestRunModel().getPostRunAction();
    }

    public String getTrendReportId()
    {
        return getPcTestRunModel().getTrendReportId(true);
    }

    public String getAutoTestInstanceID()
    {
        return getPcTestRunModel().getAutoTestInstanceID();
    }
    public String getTestInstanceId()
    {
        return getPcTestRunModel().getTestInstanceId();
    }


    public String getAddRunToTrendReport()
    {
        return getPcTestRunModel().getAddRunToTrendReport();
    }


    public boolean isVudsMode()
    {
        return getPcTestRunModel().isVudsMode();
    }

    public String getRetry () {
        return getPcTestRunModel().getRetry();
    }

    public int getRetryOccurrences () {
        return getPcTestRunModel().getRetryOccurrences();
    }

    public int getRetryDelay () {
        return getPcTestRunModel().getRetryDelay();
    }

    public String getDescription()
    {
        return getPcTestRunModel().getDescription();
    }

    public boolean isHTTPSProtocol()
    {
        return getPcTestRunModel().httpsProtocol();
    }

    public boolean isStatusBySLA() {
        return statusBySLA;
    }

    public String getProxyOutURL(){ return getPcTestRunModel().getProxyOutURL();}


    // This indicates to Jenkins that this is an implementation of an extension
    // point
    @Extension
    @Symbol("pcBuild")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {

            load();
        }

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

        @Override
        public String getDisplayName() {

            return "Run performance test using Performance Center";
        }

        public FormValidation doCheckPcServerName(@QueryParameter String value) {

            return validateString(value, "PC Server");
        }

        public FormValidation doCheckAlmDomain(@QueryParameter String value) {

            return validateString(value, "Domain");
        }

        public FormValidation doCheckAlmProject(@QueryParameter String value) {

            return validateString(value, "Project");
        }

        public FormValidation doCheckTestId(@QueryParameter String value) {

            return validateHigherThanInt(value, "Test ID", 0, true);
        }

        public FormValidation doCheckRetryDelay(@QueryParameter String value) {

            return validateHigherThanInt(value, "Delay between attempts (in minutes)", 0, true);
        }

        public FormValidation doCheckRetryOccurrences(@QueryParameter String value) {

            return validateHigherThanInt(value, "Number of attempts", 0, true);
        }

        // if autoTestInstanceID is selected we don't need to check the validation of the test instance
//        public static FormValidation CheckOnlyAutoTestInstanceId(String autoTestInstanceID){
//            if(autoTestInstanceID.equals("AUTO"))
//                return FormValidation.ok();
//            else
//                return FormValidation.error("Error ");
//        }



        public FormValidation doCheckTestInstanceId(@QueryParameter String value){
            return validateHigherThanInt(value, "Test Instance ID", 0, true);
        }


        public FormValidation doCheckTimeslotDuration(@QueryParameter TimeslotDuration value) {

            return validateHigherThanInt(
                    String.valueOf(value.toMinutes()),
                    "Timeslot Duration (in minutes)",
                    30,
                    false);
        }

        public FormValidation doCheckTimeslotId(@QueryParameter String value) {

            return validateHigherThanInt(value, "Timeslot ID", 0, true);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item project,
                                                   @QueryParameter String pcUrl,
                                                   @QueryParameter String value) {
            return checkCredentialsId(project, pcUrl, value);
        }

        public FormValidation doCheckCredentialsProxyId(@AncestorInPath Item project,
                                                        @QueryParameter String pcUrl,
                                                        @QueryParameter String value) {
            return checkCredentialsId(project, pcUrl, value);
        }

        public FormValidation checkCredentialsId(@AncestorInPath Item project,
                                                 @QueryParameter String pcUrl,
                                                 @QueryParameter String credentialIdValue) {
            if (project == null || !project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }

            String credentialIdValueStr = Util.fixEmptyAndTrim(credentialIdValue);
            if (credentialIdValueStr == null) {
                return FormValidation.ok();
            }

            String pcUrlStr = Util.fixEmptyAndTrim(pcUrl);
            if (pcUrlStr == null)
            // not set, can't check
            {
                return FormValidation.ok();
            }

            if (pcUrlStr.indexOf('$') >= 0)
            // set by variable, can't check
            {
                return FormValidation.ok();
            }

            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    project,
                    project instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) project) : ACL.SYSTEM,
                    URIRequirementBuilder.create().build(),
                    new IdMatcher(credentialIdValueStr))) {

                if (StringUtils.equals(credentialIdValueStr, o.value)) {
                    return FormValidation.ok();
                }
            }
            // no credentials available, can't check
            return FormValidation.warning("Cannot find any credentials with id " + credentialIdValueStr);
        }


        /**
         * @param limitIncluded
         *            if true, value must be higher than limit. if false, value must be equal to or
         *            higher than limit.
         */
        private FormValidation validateHigherThanInt(
                String value,
                String field,
                int limit,
                boolean limitIncluded) {
            FormValidation ret = FormValidation.ok();
            value = value.trim();
            String messagePrefix = field + " must be ";
            if (StringUtils.isBlank(value)) {
                ret = FormValidation.error(messagePrefix + "set");
            } else {
                try {
                    //regular expression: parameter (with brackets or not)
                    if (value.matches("^\\$\\{[\\w-. ]*}$|^\\$[\\w-.]*$"))
                        return ret;
                        //regular expression: number
                    else if (value.matches("[0-9]*$|")) {
                        if (limitIncluded && Integer.parseInt(value) <= limit)
                            ret = FormValidation.error(messagePrefix + "higher than " + limit);
                        else if (Integer.parseInt(value) < limit)
                            ret = FormValidation.error(messagePrefix + "at least " + limit);
                    }
                    else
                        ret = FormValidation.error(messagePrefix + "a whole number or a parameter, e.g.: 23, $TESTID or ${TEST_ID}.");
                } catch (Exception e) {
                    ret = FormValidation.error(messagePrefix + "a whole number or a parameter (e.g.: $TESTID or ${TestID})");
                }
            }

            return ret;

        }

        private FormValidation validateString(String value, String field) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(value.trim())) {
                ret = FormValidation.error(field + " must be set");
            }

            return ret;
        }


        public List<PostRunAction> getPostRunActions() {

            return PcTestRunModel.getPostRunActions();
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

    }

}
