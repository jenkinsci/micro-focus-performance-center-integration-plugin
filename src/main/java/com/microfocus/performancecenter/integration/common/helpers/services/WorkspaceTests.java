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

package com.microfocus.performancecenter.integration.common.helpers.services;


import com.microfocus.performancecenter.integration.common.helpers.constants.PcTestRunConstants;
import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFile;
import com.microfocus.performancecenter.integration.common.helpers.utils.Helper;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;
import hudson.Extension;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

@Extension
public class WorkspaceTests {

    private static boolean verifyFileIsTest(Path fileFullPath, Path workspace, boolean considerXML) {
        if (fileFullPath == null || fileFullPath.getParent() == null || workspace == null || fileFullPath.getParent().equals(workspace)) {
            return false;
        }

        boolean verifyFileIsTest = false;
        Path test = Paths.get(fileFullPath.toString());
        if (isNotDirectlyUnderRootWorkspace(test, workspace) && isParentsNotScript(test, workspace) && isPossiblyTest(test, considerXML)) {
            verifyFileIsTest = true;
        }

        return verifyFileIsTest;
    }

    //verify that the file does not belong to a script
    private static boolean isParentsNotScript(Path fullPath, Path workspace) {

        boolean isParentsNotScript = false;

        if (fullPath == null || workspace == null || !isChild(fullPath, workspace))
            return false;

        if ((Files.isDirectory(fullPath) && fullPath.equals(workspace)))
            return true;

        isParentsNotScript = isParentsNotScript(fullPath, workspace, isParentsNotScript);

        return isParentsNotScript;
    }

    private static boolean isParentsNotScript(Path fullPath, Path workspace, boolean isParentsNotScript) {
        if (fullPath != null) {
            Path parentPath = fullPath.getParent();
            if(parentPath != null && workspace != null) {
                File[] files = lrSupportedScriptSignatureFinder(parentPath.toString());
                boolean parentPathContainsLrSupportedScriptSignature = files != null ? (files.length > 0) : false;

                // Directories and subdirectories are verified
                Path parentParentPath = parentPath.getParent();
                if (parentParentPath != null) {
                    if (!parentParentPath.equals(workspace) && !parentPathContainsLrSupportedScriptSignature)
                        isParentsNotScript = isParentsNotScript(parentPath, workspace);
                    else if (parentParentPath.equals(workspace) && !parentPathContainsLrSupportedScriptSignature)
                        isParentsNotScript = true;
                }
            }
        }
        return isParentsNotScript;
    }



    //verify that file has xml (if considerXML is true) or yaml extension
    private static boolean isPossiblyTest(Path fullPath, boolean considerXML) {
        return Files.isRegularFile(fullPath)
                && ((fullPath.toString().endsWith(PcTestRunConstants.XML_EXTENSION) && considerXML) || fullPath.toString().endsWith(PcTestRunConstants.YAML_EXTENSION) || fullPath.toString().endsWith(PcTestRunConstants.YML_EXTENSION));
    }

    //verify fullpath is not a file right under the workspace
    private static boolean isNotDirectlyUnderRootWorkspace(Path fullPath, Path workspace) {
        //workspace itself not verified
        if (fullPath == null || workspace == null || (Files.isDirectory(fullPath) && fullPath.equals(workspace)))
            return false;

        Path parentPath = Helper.getParent(fullPath);

        //files under workspace directory are ignored
        return !(Files.isRegularFile(fullPath) && Files.isDirectory(parentPath) && parentPath.equals(workspace));

    }

    //get files ending with usr or jmx extension under specific directory
    private static File[] lrSupportedScriptSignatureFinder(String dirName) {
        File dir = new File(dirName);

        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return (filename.toLowerCase().endsWith(PcTestRunConstants.USR_EXTENSION)
                        || filename.toLowerCase().endsWith(PcTestRunConstants.JMX_EXTENSION)
                        || filename.toLowerCase().endsWith(PcTestRunConstants.GATLING_EXTENSION)
                        || ((PcTestRunConstants.DEVWEB_MAIN_FILE.equalsIgnoreCase(filename)) && rstFinder(dir.toString()).length > 0)
                        || (filename.toLowerCase().endsWith(PcTestRunConstants.SELENIUM_EXTENSION))
                );
            }
        });
    }

    private static File[] rstFinder(String dirName) {
        File dir = new File(dirName);
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return (PcTestRunConstants.DEVWEB_RTS_FILE.equalsIgnoreCase(filename));
            }
        });
    }

    //verify child path is subdirectory of parent
    private static boolean isChild(Path child, Path parent) {
        return child.toAbsolutePath().startsWith(parent.toAbsolutePath());
    }

    public SortedSet<AffectedFile> getAllAffectedFiles(Set<ModifiedFile> allModifiedFiles, Path workspace) {
        SortedSet<AffectedFile> result = new TreeSet<>();

        allModifiedFiles.stream()
                .map(file -> file.getFullPath())
                .filter(file -> WorkspaceTests.isNotDirectlyUnderRootWorkspace(file, workspace))
                .map(file -> new AffectedFile(file, workspace))
                .forEachOrdered(result::add);

        return result;
    }

    public SortedSet<AffectedFile> getAllTestsToCreateOrUpdate(Path workspace, boolean considerXML) throws IOException {
        SortedSet<AffectedFile> result = new TreeSet<>();

        try (Stream<Path> stream = Files.walk(workspace)) {
            stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(file -> WorkspaceTests.verifyFileIsTest(file, workspace, considerXML))
                    .map(file -> new AffectedFile(file, workspace))
                    .forEachOrdered(result::add);
        }

        return result;
    }

    public SortedSet<AffectedFile> getAllTestsToCreateOrUpdate(Set<AffectedFile> allAffectedFiles, Path workspace, boolean considerXML) {
        SortedSet<AffectedFile> result = new TreeSet<>();

        allAffectedFiles.stream()
                .map(affectedFile -> getOptionalPathIfFileIsTest(affectedFile.getFullPath(), workspace, considerXML))
                .filter(Optional::isPresent)
                .map(testFile -> new AffectedFile(testFile.get(), workspace))
                .forEachOrdered(result::add);
        return result;
    }

    private Optional<Path> getOptionalPathIfFileIsTest(Path fileFullPath, Path workspace, boolean considerXML) {
        if (fileFullPath == null || fileFullPath.getParent() == null || workspace == null || fileFullPath.getParent().equals(workspace)) {
            return Optional.empty();
        }

        Optional<Path> testToReturn = Optional.empty();
        Path test = Paths.get(fileFullPath.toString());
        if (isNotDirectlyUnderRootWorkspace(test, workspace) && isParentsNotScript(test, workspace) && isPossiblyTest(test, considerXML)) {
            testToReturn = Optional.of(test);
        }

        if (testToReturn.isPresent()) {
            return Optional.of(fileFullPath);
        }

        return testToReturn;
    }
}
