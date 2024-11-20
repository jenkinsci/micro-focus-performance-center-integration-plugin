package com.microfocus.performancecenter.integration.pcgitsync.helper;

import java.io.IOException;
import java.util.Set;

import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcException;
import com.microfocus.adm.performancecenter.plugins.common.pcentities.PcScript;
import com.microfocus.adm.performancecenter.plugins.common.rest.PcRestProxy;
import com.microfocus.performancecenter.integration.common.helpers.compressor.ICompressor;
import com.microfocus.performancecenter.integration.common.helpers.utils.Helper;
import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;
import hudson.model.Result;
import hudson.model.TaskListener;

import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFolder;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;
import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.logStackTrace;

public class ScriptUploader {
    private final TaskListener listener;
    private final ConfigureSystemSection configureSystemSection;
    private final Set<AffectedFolder> scriptsForUpload;
    private final PcRestProxy restProxy;
    private final boolean allowFolderCreation;
    private Result result;
    private final String subjectTestPlan;
    private final boolean uploadRunTimeFiles;
    private final ICompressor compressor;
    public ScriptUploader(TaskListener listener,
                          ConfigureSystemSection configureSystemSection,
                          Set<AffectedFolder> scriptsForUpload,
                          PcRestProxy restProxy,
                          boolean allowFolderCreation,
                          Result result,
                          String subjectTestPlan,
                          boolean uploadRunTimeFiles,
                          ICompressor compressor)
    {
        this.listener = listener;
        this.configureSystemSection = configureSystemSection;
        this.scriptsForUpload = scriptsForUpload;
        this.restProxy = restProxy;
        this.allowFolderCreation = allowFolderCreation;
        this.result = result;
        this.subjectTestPlan = subjectTestPlan;
        this.uploadRunTimeFiles = uploadRunTimeFiles;
        this.compressor = compressor;
    }

    public Result uploadScripts() throws InterruptedException {
        for (AffectedFolder script : scriptsForUpload) {
            Thread.sleep(50);
            result = result.combine(uploadScript(script));
        }
        return  result;
    }

    private Result uploadScript(AffectedFolder script)
            throws InterruptedException {
        Result resultToReturn = result;
        String scriptFullPath = script.getFullPath().toString();
        String scriptRelativePath = script.getRelativePath().toString();
        try {
            Thread.sleep(50);
            String archive = scriptFullPath + ".zip";
            compressor.compressDirectoryToFile(scriptFullPath, archive, true, "JENKINS PLUGIN");
            String targetSubject = allowFolderCreation ? script.getSubjectPath() : subjectTestPlan;
            Thread.sleep(50);
            try {
                int scriptId = restProxy.uploadScript(targetSubject, true, uploadRunTimeFiles, true, archive);
                log(
                        listener,
                        "Uploading script '%s' from Git to project...",
                        true,
                        scriptRelativePath
                );
                if (scriptId != 0) {
                    PcScript pcScript = restProxy.getScript(scriptId);
                    log(
                            listener,
                            "+++++ Script uploaded successfully: '%s\\%s' (ID: %d, protocol: %s, mode: %s).",
                            false,
                            pcScript.getTestFolderPath(),
                            pcScript.getName(),
                            pcScript.getID(),
                            pcScript.getProtocol(),
                            pcScript.getWorkingMode()
                    );
                } else {
                    resultToReturn = reportFailureOnUploadScript(resultToReturn, scriptFullPath, scriptRelativePath, null);
                }
            } catch (PcException ex) {
                resultToReturn = reportFailureOnUploadScript(resultToReturn, scriptFullPath, scriptRelativePath, ex);
            }
            Thread.sleep(50);
        } catch (IOException ex) {
            resultToReturn = reportFailureOnUploadScript(resultToReturn, scriptFullPath, scriptRelativePath, ex);
        } finally {
            log(
                    listener,
                    "",
                    false
            );
        }
        return resultToReturn;
    }

    private Result reportFailureOnUploadScript(Result resultToReturn, String scriptFullPath, String scriptRelativePath, Exception ex) {
        if (Helper.isUsrScript(scriptFullPath)) {
            resultToReturn = Result.FAILURE;
            log(
                    listener,
                    "***** Failed to upload the script %s.",
                    true,
                    scriptRelativePath
            );
            if (ex != null) {
                log(
                        listener,
                        "***** Script Upload of %s failed with Error: %s.",
                        false,
                        scriptRelativePath,
                        ex.getMessage()
                );
            }
        } else {
            log(
                    listener,
                    "----- Failed to upload the script %s but since this is a custom script (possibly introduced in latest versions of LRE), the build will not fail.",
                    false,
                    scriptRelativePath
            );
            if (ex != null) {
                log(
                        listener,
                        "***** Script upload %s of failed with Error: %s.",
                        false,
                        scriptRelativePath,
                        ex.getMessage()
                );
            }
        }
        if (ex != null) {
            logStackTrace(listener, configureSystemSection, ex);
        }
        return resultToReturn;
    }
}
