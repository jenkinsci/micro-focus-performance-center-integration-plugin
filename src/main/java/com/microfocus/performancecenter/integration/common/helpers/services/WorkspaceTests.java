package com.microfocus.performancecenter.integration.common.helpers.services;


import com.microfocus.performancecenter.integration.common.helpers.constants.PcTestRunConstants;
import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFile;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;
import hudson.Extension;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Extension
public class WorkspaceTests {

    public SortedSet<AffectedFile> getAllAffectedFiles(Set<ModifiedFile> allModifiedFiles, Path workspace) {
        SortedSet<AffectedFile> result = new TreeSet<>();

        allModifiedFiles.stream()
                .map(file -> file.getFullPath())
                .filter(file -> WorkspaceTests.isNotDirectlyUnderRootWorkspace(file, workspace))
                .map(file -> new AffectedFile(file, workspace))
                .forEachOrdered(result::add);

        return result;
    }

    public SortedSet<AffectedFile> getAllTestsToCreateOrUpdate(Path workspace) throws IOException {
        SortedSet<AffectedFile> result = new TreeSet<>();

        try ( Stream<Path> stream = Files.walk(workspace)) {
            stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(file -> WorkspaceTests.verifyFileIsTest(file, workspace))
                    .map(file -> new AffectedFile(file, workspace))
                    .forEachOrdered(result::add);
        }

        return result;
    }

    public SortedSet<AffectedFile> getAllTestsToCreateOrUpdate(Set<AffectedFile> allAffectedFiles, Path workspace) {
        SortedSet<AffectedFile> result = new TreeSet<>();

        allAffectedFiles.stream()
                .map(affectedFile -> getOptionalPathIfFileIsTest(affectedFile.getFullPath(), workspace))
                .filter(Optional::isPresent)
                .map(testFile -> new AffectedFile(testFile.get(), workspace))
                .forEachOrdered(result::add);
        return result;
    }


    private Optional<Path> getOptionalPathIfFileIsTest(Path fileFullPath, Path workspace) {
        if (fileFullPath == null || fileFullPath.getParent().equals(workspace) ) {
            return Optional.empty();
        }

        Optional<Path> testToReturn = Optional.empty();
        Path test = Paths.get(fileFullPath.toString());
        if (isNotDirectlyUnderRootWorkspace(test, workspace) && isParentsNotScript(test, workspace) && isPossiblyTest(test)) {
            testToReturn = Optional.of(test);
        }

        if (testToReturn.isPresent()) {
            return Optional.of(fileFullPath);
        }

        return testToReturn;
    }

    private static boolean verifyFileIsTest(Path fileFullPath, Path workspace) {
        if (fileFullPath == null || fileFullPath.getParent().equals(workspace) ) {
            return false;
        }

        boolean verifyFileIsTest = false;
        Path test = Paths.get(fileFullPath.toString());
        if (isNotDirectlyUnderRootWorkspace(test, workspace) && isParentsNotScript(test, workspace) && isPossiblyTest(test)) {
            verifyFileIsTest = true;
        }

        return verifyFileIsTest;
    }

    //verify that the file does not belong to a script
    private static boolean isParentsNotScript(Path fullPath, Path workspace) {

        boolean isParentsNotScript = false;

        if(fullPath == null || workspace == null || !isChild(fullPath, workspace))
            return false;

        if( (Files.isDirectory(fullPath) && fullPath.equals(workspace)))
            return true;

        Path parentPath = fullPath.getParent();
        File[] files = finder(parentPath.toString());
        boolean parentPathContainsUsrOrJmxfiles = files != null ? (files.length > 0) : false;

        //directories and subdirectories are verified
        if (!parentPath.getParent().equals(workspace) && !parentPathContainsUsrOrJmxfiles)
            isParentsNotScript = isParentsNotScript(parentPath, workspace);
        else if (parentPath.getParent().equals(workspace) && !parentPathContainsUsrOrJmxfiles)
            isParentsNotScript = true;

        return isParentsNotScript;
    }

    //verify that file has xml or yaml extension
    private static boolean isPossiblyTest(Path fullPath) {
        return Files.isRegularFile(fullPath)
                && (fullPath.toString().endsWith(PcTestRunConstants.XML_EXTENSION) || fullPath.toString().endsWith(PcTestRunConstants.YAML_EXTENSION) || fullPath.toString().endsWith(PcTestRunConstants.YML_EXTENSION));
    }

    //verify fullpath is not a file right under the workspace
    private static boolean isNotDirectlyUnderRootWorkspace(Path fullPath, Path workspace) {
        //workspace itself not verified
        if(fullPath == null || workspace == null || (Files.isDirectory(fullPath) && fullPath.equals(workspace)))
            return false;

        Path parentPath = fullPath.getParent();

        //files under workspace directory are ignored
        if(Files.isRegularFile(fullPath) && Files.isDirectory(parentPath) && parentPath.equals(workspace))
            return false;

        return true;
    }

    //get files ending with usr or jmx extension under specific directory
    private static File[] finder( String dirName){
        File dir = new File(dirName);

        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            { return (filename.endsWith(PcTestRunConstants.USR_EXTENSION) || filename.endsWith(PcTestRunConstants.JMX_EXTENSION)) ; }
        } );
    }

    //verify child path is subdirectory of parent
    private static boolean isChild(Path child, Path parent) {
        return child.toAbsolutePath().startsWith(parent.toAbsolutePath());
    }

}
