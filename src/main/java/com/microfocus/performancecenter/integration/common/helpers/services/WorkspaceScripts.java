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

import hudson.Extension;
import com.microfocus.performancecenter.integration.common.helpers.utils.AffectedFolder;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedType;
import com.microfocus.performancecenter.integration.common.helpers.utils.ModifiedFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Extension
public class WorkspaceScripts {

    public SortedSet<AffectedFolder> getAllScriptsForUpload(Path workspace) throws IOException {
        SortedSet<AffectedFolder> result = new TreeSet<>();

        try ( Stream<Path> stream = Files.walk(workspace)) {
            stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(WorkspaceScripts::isScript)
                    .map(file -> new AffectedFolder(file.getParent(), workspace))
                    .forEachOrdered(result::add);
        }

        return result;
    }

    public SortedSet<AffectedFolder> getAllAffectedFolders(Set<ModifiedFile> allModifiedFiles, Path workspace) {
        SortedSet<AffectedFolder> result = new TreeSet<>();

        allModifiedFiles.stream()
                .map(changedFile -> changedFile.getFullPath().getParent())
                .map(parent -> new AffectedFolder(parent, workspace))
                .forEachOrdered(result::add);

        return result;
    }

    public SortedSet<AffectedFolder> getAllScriptsForDelete(Set<ModifiedFile> allModifiedFiles, Path workspace) {
        SortedSet<AffectedFolder> result = new TreeSet<>();

        allModifiedFiles.stream()
                .filter(modifiedFile -> modifiedFile.getModifiedType() == ModifiedType.DELETED)
                .filter(changedFile -> isScript(changedFile.getFullPath()))
                .map(changedFile -> changedFile.getFullPath().getParent())
                .filter(parent -> !Files.exists(parent))
                .map(parent -> new AffectedFolder(parent, workspace))
                .forEachOrdered(result::add);

        return result;
    }

    public SortedSet<AffectedFolder> getAllScriptsForUpload(Set<AffectedFolder> allAffectedFolders, Path workspace) {
        SortedSet<AffectedFolder> result = new TreeSet<>();

        allAffectedFolders.stream()
                .map(affectedFolder -> getScriptFolderForSubfolder(affectedFolder.getFullPath(), workspace))
                .filter(Optional::isPresent)
                .map(scriptFolder -> new AffectedFolder(scriptFolder.get(), workspace))
                .forEachOrdered(result::add);

        return result;
    }

    // Suppose we have folder1/folder2/folder3 structure
    // And the only .usr file here is folder1/UC.usr (i.e. folders 2 and 3 do not contain it)
    // And we pass "folder3" as a subfolder parameter,
    // This method returns "folder1"
    private Optional<Path> getScriptFolderForSubfolder(Path subfolderFullPath, Path workspace) {
        if (subfolderFullPath == null || subfolderFullPath.equals(workspace)) {
            return Optional.empty();
        }

        Optional<Path> script;
        try {
            try (Stream<Path> stream = Files.list(subfolderFullPath)) {
                script = stream
                        .filter(WorkspaceScripts::isScript)
                        .findFirst();
            }
        } catch (IOException ioe) {
            script = Optional.empty();
        }

        if (script.isPresent()) {
            return Optional.of(subfolderFullPath);
        }

        return getScriptFolderForSubfolder(subfolderFullPath.getParent(), workspace);
    }

    private static boolean isScript(Path fullPath) {
        return !Files.isDirectory(fullPath) && (fullPath.toString().toLowerCase(Locale.ROOT).endsWith(".usr") || fullPath.toString().toLowerCase(Locale.ROOT).endsWith(".jmx"));
    }

}
