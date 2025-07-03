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

package com.microfocus.performancecenter.integration.pctestrun;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PostRunAction;

import java.io.PrintStream;

public interface PcTestBase {

    String SERVER_AND_PORT = "jenkins.server:8082";
    String PC_SERVER_NAME = "pcServer.hp.com";
    String CREDENTIALSID = "123456789";
    String ALM_DOMAIN = "ALMDOM";
    String ALM_PROJECT = "ALMPROJ";
    String TEST_TO_RUN = "EXISTING_TEST";
    String TEST_ID = "1";
    String TEST_CONTENT_TO_CREATE = "";
    String TEST_INSTANCE_ID = "2";
    String TIMESLOT_DURATION_HOURS = "0";
    String TIMESLOT_DURATION_MINUTES = "34";
    String TIMESLOT_ID = "56";
    PostRunAction POST_RUN_ACTION = PostRunAction.COLLATE_AND_ANALYZE;
    boolean VUDS_MODE = false;
    String DESCRIPTION = "Testing HPE Performance Center Jenkins plugin";
    String RUN_ID = "7";
    String RUN_ID_WAIT = "8";
    String REPORT_ID = "9";
    String NV_INSIGHTS_REPORT_ID = "10";
    String STOP_MODE = "stop";
    String WEB_PROTOCOL = "http";
    Boolean IS_HTTPS = false;
    String TESTINSTANCEID = "MANUAL";
    PrintStream LOGGER = null;
    String RETRY = "NO_RETRY";
    String RETRYDELAY = "5";
    String RETRYOCCURRENCES = "3";
    String TRENDREPORTWAITTIME = "0";
    boolean AUTHENTICATE_WITH_TOKEN = false;
    boolean SEARCH_TIMESLOT = false;

    MockPcTestRunModel pcModel = new MockPcTestRunModel(SERVER_AND_PORT, PC_SERVER_NAME, CREDENTIALSID, ALM_DOMAIN, ALM_PROJECT,
            TEST_TO_RUN, TEST_ID, TEST_CONTENT_TO_CREATE, TESTINSTANCEID, TEST_INSTANCE_ID,
            TIMESLOT_DURATION_HOURS,
            TIMESLOT_DURATION_MINUTES, POST_RUN_ACTION,
            VUDS_MODE, DESCRIPTION, IS_HTTPS, RETRY, RETRYDELAY, RETRYOCCURRENCES, TRENDREPORTWAITTIME, AUTHENTICATE_WITH_TOKEN, SEARCH_TIMESLOT);

    String runResponseEntity = "<Run xmlns=\"http://www.hp.com/PC/REST/API\">" +
            "<TestID>" + TEST_ID + "</TestID>" +
            "<TestInstanceID>" + TEST_INSTANCE_ID + "</TestInstanceID>" +
            "<PostRunAction>" + POST_RUN_ACTION.getValue() + "</PostRunAction>" +
            "<TimeslotID>1076</TimeslotID>" +
            "<VudsMode>false</VudsMode>" +
            "<ID>" + RUN_ID + "</ID>" +
            "<Duration>" + TIMESLOT_DURATION_MINUTES + "</Duration>" +
            "<RunState>*</RunState>" +
            "<RunSLAStatus />" +
            "</Run>";

    String emptyResultsEntity = "<RunResults xmlns=\"http://www.hp.com/PC/REST/API\" />";

    String runResultsEntity = "<RunResults xmlns=\"http://www.hp.com/PC/REST/API\">" +
            "<RunResult>" +
            "<ID>1302</ID>" +
            "<Name>output.mdb.zip</Name>" +
            "<Type>Output Log</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "<RunResult>" +
            "<ID>1303</ID>" +
            "<Name>RawResults.zip</Name>" +
            "<Type>Raw Results</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "<RunResult>" +
            "<ID>1304</ID>" +
            "<Name>Results.zip</Name>" +
            "<Type>Analyzed Result</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "<RunResult>" +
            "<ID>" + REPORT_ID + "</ID>" +
            "<Name>Reports.zip</Name>" +
            "<Type>HTML Report</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "<RunResult>" +
            "<ID>" + NV_INSIGHTS_REPORT_ID + "</ID>" +
            "<Name>NVInsights.zip</Name>" +
            "<Type>HTML REPORT</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "<RunResult>" +
            "<ID>1306</ID>" +
            "<Name>HighLevelReport_7.xls</Name>" +
            "<Type>Rich Report</Type>" +
            "<RunID>" + RUN_ID + "</RunID>" +
            "</RunResult>" +
            "</RunResults>";

    String pcAuthenticationFailureMessage = "Exception of type 'HPE.PC.API.Model.Exceptions.InvalidAuthenticationDataException' was thrown. Error code: 1100";

    String pcNoTimeslotExceptionMessage = "Failed to retrieve reservation information for reservation " + TIMESLOT_ID + ". Error code: 1202";

    String pcStopNonExistRunFailureMessage = "Failed to retrieve run " + RUN_ID + " information from domain " + ALM_DOMAIN + ", project " + ALM_PROJECT + ". Error code: 1300";

    String testResponseEntity = "<Test>" +
            "<ID>2</ID>" +
            "<Name>test1</Name>" +
            "</Test>";

}