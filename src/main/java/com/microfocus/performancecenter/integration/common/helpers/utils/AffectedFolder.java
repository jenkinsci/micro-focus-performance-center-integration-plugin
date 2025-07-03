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

package com.microfocus.performancecenter.integration.common.helpers.utils;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class AffectedFolder implements Comparable<AffectedFolder> {

    @NonNull
    private final Path fullPath;

    @NonNull
    private final Path relativePath;

    public AffectedFolder(Path folderFullPath, Path workspace) {
        this.fullPath = Objects.requireNonNull(folderFullPath);
        PathVerifier.requireAbsolute(folderFullPath, "Folder");
        PathVerifier.requireAbsolute(workspace, "Workspace");

        this.relativePath = workspace.relativize(folderFullPath);
    }

    public String getSubjectPath() {
        String subjectRoot = "Subject";
        String scriptPath = subjectRoot + "\\" + relativePath.toString().replace(File.separatorChar, '\\');
        Path script = Paths.get(scriptPath);
        if (Helper.getParent(script).toString().equals(subjectRoot))
            return scriptPath;
        else
            return Helper.getParent(script).toString();
    }

    @Override
    public int compareTo(AffectedFolder o) {
        return relativePath.compareTo(o.getRelativePath());
    }

    @Override
    public String toString() {
        // we override the default relative representation of the root folder itself
        // for the sake of readability:
        return "/" + relativePath.toString();
    }

    public String toString(boolean backslash) {
        if (backslash)
            return "\\" + relativePath.toString().replace("/", "\\");
        return "//" + relativePath.toString().replace("\\", "//");
    }

    @NonNull
    public Path getFullPath() {
        return this.fullPath;
    }

    @NonNull
    public Path getRelativePath() {
        return this.relativePath;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AffectedFolder)) return false;
        final AffectedFolder other = (AffectedFolder) o;
        return this.getRelativePath().equals(other.getRelativePath());
    }

    public int hashCode() {
        final int PRIME = 59;
        return PRIME + this.getRelativePath().hashCode();
    }
}
