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

package com.microfocus.performancecenter.integration.common.helpers.services;

import com.cloudbees.jenkins.plugins.changelog.Changes;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedType;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.CheckForNull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.microfocus.performancecenter.integration.common.helpers.utils.LogHelper.log;

@Extension
public class ModifiedFiles {

    private boolean addDate = true;
    @CheckForNull
    public SortedSet<ModifiedFile> getModifiedFilesSinceLastSuccess(TaskListener listener, Run<?, ?> current, String remoteWorkspacePath) {
        initMessage(listener, "Beginning to analyze modifications made in GIT repository since last successful build", true);

        Run<?, ?> lastSuccess = current.getPreviousSuccessfulBuild();

        if (lastSuccess == null) {
            log(listener, "No previously successful build was found. All scripts will be uploaded.", addDate);
            return null;
        }
        if (current instanceof WorkflowRun) {
            log(listener, "Because this is being run through a pipeline job, no Comparison will be made to previous build and all scripts will be uploaded to LRE project.", addDate);
            return null;
        }
        log(listener, "The last successful build was found (ID = %d). Only modified scripts will be loaded", addDate, lastSuccess.getNumber());

        List<ChangeLogSet> changeLogList = new Changes((AbstractBuild) current, lastSuccess.getNumber() + 1).getChanges();
        return getAllChangedFiles(Paths.get(remoteWorkspacePath), changeLogList);
    }

    private SortedSet<ModifiedFile> getAllChangedFiles(Path workspace, List<ChangeLogSet> changelog) {
        SortedSet<ModifiedFile> result = new TreeSet<>();

        changelog.forEach((change) -> {
            for (Object item : change.getItems()) {
                ChangeLogSet.Entry e = (ChangeLogSet.Entry) item;
                e.getAffectedFiles()
                        .forEach((file) -> {
                            result.add(
                                    new ModifiedFile(
                                            ModifiedType.from(file.getEditType()),
                                            // we hardcoded '/' here because it is used regardless
                                            // of target platform, by contract. 
                                            // see hudson.​scm.​ChangeLogSet.​AffectedFile#getPath()
                                            Paths.get(file.getPath().replace('/', File.separatorChar)),
                                            workspace
                                    )
                            );
                        });
            }
        });

        return result;
    }

    public static void initMessage(TaskListener listener, String message, boolean doubleStarlineWithoutSpace) {
        String spaces = doubleStarlineWithoutSpace? "" : "         ";
        String stars = message.replaceAll(".", "*");
        if(doubleStarlineWithoutSpace) {
            log(listener,"", true);
            log(listener, spaces.concat(stars), false);
        }
        log(listener, spaces.concat(stars), false);
        log(listener, spaces.concat(message), false);
        log(listener, spaces.concat(stars), false);
        if(doubleStarlineWithoutSpace) {
            log(listener, spaces.concat(stars), false);
            log(listener,"", true);
        }
        log(listener, "", false);
    }

}
