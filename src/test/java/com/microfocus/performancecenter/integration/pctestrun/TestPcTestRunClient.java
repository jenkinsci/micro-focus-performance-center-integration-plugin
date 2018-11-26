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

package com.microfocus.performancecenter.integration.pctestrun;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcRunResponse;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.RunState;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import hudson.FilePath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings({"squid:S2699","squid:S3658"})
public class TestPcTestRunClient {

    private static PcTestRunClient pcTestRunClient;
    public final String RESOURCES_DIR = getClass().getResource("").getPath();

    @BeforeClass
    public static void setUp() {
        try {
            PcRestProxy resetProxy = new MockPcRestProxy(PcTestBase.WEB_PROTOCOL, PcTestBase.PC_SERVER_NAME, PcTestBase.ALM_DOMAIN,
                    PcTestBase.ALM_PROJECT,PcTestBase.LOGGER);
            pcTestRunClient = new PcTestRunClient(PcTestBase.pcModel, resetProxy);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testLogin(){
        System.out.println("Testing Login to PC server");
        boolean loginSucceded = pcTestRunClient.login(null);
        Assert.assertTrue("Failed to login with pcTestRunClient", true);
    }

    @Test
    public void testStartRun(){
        System.out.println("Testing Start Run with PC client");
        try {
            Assert.assertTrue("Failed to start run with pcTestRunClient", pcTestRunClient.startRun() > 0);
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test (timeout=5000)
    public void testWaitForRunCompletion(){

        System.out.println("Testing Wait for Run Completion with PC client");
        try {
            PcRunResponse response = pcTestRunClient.waitForRunCompletion(Integer.parseInt(PcTestBase.RUN_ID_WAIT), 200);
            Assert.assertEquals(response.getRunState(), RunState.FINISHED.value());
        } catch (InterruptedException e) {
            Assert.fail("pcTestRunClient did not return from waitForRunCompletion (test run has timed out)");
        }catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testPublishRunReport(){

        System.out.println("Testing Publish PC Run Report to Jenkins server with PC client");
        try {

            FilePath reportHtml = pcTestRunClient.publishRunReport(Integer.parseInt(PcTestBase.RUN_ID),
                    String.format(PcTestRunBuilder.getRunReportStructure(), RESOURCES_DIR, PcTestRunBuilder.getArtifactsDirectoryName(), PcTestBase.RUN_ID));
            Assert.assertTrue("Failed to publish PC run report", reportHtml.exists());
            try {
                // Test cleanup
                reportHtml.getParent().getParent().getParent().deleteRecursive();
            } catch (Exception e) {
            }
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void testLogout() {
        System.out.println("Testing Logout from PC server");
        Assert.assertTrue("Failed to logout with pcTestRunClient", pcTestRunClient.logout());
    }

    @Test
    public void testStopRun() {
        System.out.println("Testing Stop Run with PC client");
        Assert.assertTrue("Failed to stop run with pcTestRunClient", pcTestRunClient.stopRun(Integer.parseInt(PcTestBase.RUN_ID)));
    }


}
  
