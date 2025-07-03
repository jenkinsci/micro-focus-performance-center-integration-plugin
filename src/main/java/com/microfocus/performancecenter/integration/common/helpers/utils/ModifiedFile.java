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

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class ModifiedFile implements Comparable<ModifiedFile>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final ModifiedType modifiedType;
    private final URI fullPathAsUri;
    private final int workspaceNameCount;

    public ModifiedFile(ModifiedType modifiedType, Path relativePath, Path workspace) {
        Objects.requireNonNull(relativePath, "Relative path must not be null");
//        PathVerifier.requireAbsolute(workspace, "Workspace");
//        PathVerifier.requireRelative(relativePath, "File");

        this.modifiedType = modifiedType;
        this.fullPathAsUri = workspace.resolve(relativePath).toUri();
        this.workspaceNameCount = workspace.getNameCount();
    }

    public Path getFullPath() {
        return Paths.get(fullPathAsUri);
    }

    public Path getRelativePath() {
        Path fullPath = getFullPath();
        return fullPath.subpath(workspaceNameCount, fullPath.getNameCount());
    }

    @Override
    public int compareTo(@NonNull ModifiedFile o) {
        int compareByRelativePath = fullPathAsUri.compareTo(o.getFullPathAsUri());

        if (compareByRelativePath == 0) {
            return modifiedType.compareTo(o.getModifiedType());
        }

        return compareByRelativePath;
    }

    @Override
    public String toString() {
        return modifiedType + " " + getRelativePath();
    }

    public String toString(boolean backslash) {
        if (backslash)
            return modifiedType + " \\" + getRelativePath().toString().replace("/", "\\");
        return modifiedType + " /" + getRelativePath().toString().replace("\\", "/");
    }

    public ModifiedType getModifiedType() {
        return this.modifiedType;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ModifiedFile other)) return false;
        final Object this$modifiedType = this.getModifiedType();
        final Object other$modifiedType = other.getModifiedType();
        if (!Objects.equals(this$modifiedType, other$modifiedType))
            return false;
        final Object this$fullPathAsUri = this.getFullPathAsUri();
        final Object other$fullPathAsUri = other.getFullPathAsUri();
        return Objects.equals(this$fullPathAsUri, other$fullPathAsUri);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $modifiedType = this.getModifiedType();
        result = result * PRIME + ($modifiedType == null ? 43 : $modifiedType.hashCode());
        final Object $fullPathAsUri = this.getFullPathAsUri();
        result = result * PRIME + ($fullPathAsUri == null ? 43 : $fullPathAsUri.hashCode());
        return result;
    }

    URI getFullPathAsUri() {
        return this.fullPathAsUri;
    }

    int getWorkspaceNameCount() {
        return this.workspaceNameCount;
    }
}
