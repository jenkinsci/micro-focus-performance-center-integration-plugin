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

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcRunResponse;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import hudson.FilePath;
import org.junit.*;
import org.junit.rules.ExpectedException;

@SuppressWarnings({"squid:S2699", "squid:S3658"})
public class TestPcTestRunClientNegativeScenrios {

    private static PcTestRunClient pcTestRunClient;
    public final String RESOURCES_DIR = getClass().getResource("").getPath();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setUp() {
        System.out.println("Starting client negative testing scenarios:");
        try {
            PcRestProxy resetProxy = new MockPcRestProxyBadResponses(PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME, PcTestBase.AUTHENTICATE_WITH_TOKEN, PcTestBase.ALM_DOMAIN,
                    PcTestBase.ALM_PROJECT, PcTestBase.LOGGER);
            pcTestRunClient = new PcTestRunClient(PcTestBase.pcModel, resetProxy);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("End of client negative testing scenarios");
    }

    @Test
    public void testLoginWithWrongCredentials() {

        System.out.println("Testing Login to PC server with wrong credentials");
        Assert.assertFalse("Login to PC server with wrong creadentials should have failed", pcTestRunClient.login(null));
    }

    @Test(timeout = 5000)
    public void testHandleRunFailureWhileWaitingForRunCompletion() {

        System.out.println("Testing Wait For Run Completion with PC client while run fails");
        try {
            PcRunResponse response = pcTestRunClient.waitForRunCompletion(Integer.parseInt(PcTestBase.RUN_ID_WAIT), 200);
            Assert.assertEquals(response.getRunState(), RunState.RUN_FAILURE.value());
        } catch (InterruptedException e) {
            Assert.fail("pcTestRunClient did not return from waitForRunCompletion (test run has timed out)");
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPublishRunReportWithEmptyResults() {

        System.out.println("Testing Publish PC Run Report to while run results are empty");
        try {

            FilePath reportHtml = pcTestRunClient.publishRunReport(Integer.parseInt(PcTestBase.RUN_ID),
                    String.format(PcTestRunBuilder.getRunReportStructure(), RESOURCES_DIR, PcTestRunBuilder.getArtifactsDirectoryName(), PcTestBase.RUN_ID));
            Assert.assertNull("pcTestRunClient.publishRunReport should have returned null due to empty run results",
                    reportHtml);
        } catch (Exception e) {
            Assert.fail("pcTestRunClient.publishRunReport threw an exception (should have returned null due to empty run results): "
                    + e);
        }
    }

    @Test
    public void testPublishRunNVInsightsReportWithEmptyResults() {

        System.out.println("Testing Publish PC Run Report to while run results are empty");
        try {

            FilePath reportHtml = pcTestRunClient.publishRunReport(Integer.parseInt(PcTestBase.RUN_ID),
                    String.format(PcTestRunBuilder.getRunNVInsightsReportStructure(), RESOURCES_DIR, PcTestRunBuilder.getArtifactsDirectoryName(), PcTestBase.RUN_ID));
            Assert.assertNull("pcTestRunClient.publishRunReport should have returned null due to empty run results",
                    reportHtml);
        } catch (Exception e) {
            Assert.fail("pcTestRunClient.publishRunReport threw an exception (should have returned null due to empty run results): "
                    + e);
        }
    }

    @Test
    public void testStopNonExistingRun() {

        System.out.println("Testing stopping a non-exising run with PC client");
        Assert.assertFalse("Stopping a non-existing run should have failed", pcTestRunClient.stopRun(Integer.parseInt(PcTestBase.RUN_ID)));
    }

}
