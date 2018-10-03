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

package com.microfocus.performancecenter.integration.common.helpers.utils;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Value
@EqualsAndHashCode(of = {"modifiedType", "fullPathAsUri"})
public class ModifiedFile implements Comparable<ModifiedFile>, Serializable {

    private final ModifiedType modifiedType;

    @Getter(AccessLevel.PACKAGE)
    private final URI fullPathAsUri;

    @Getter(AccessLevel.PACKAGE)
    private final int workspaceNameCount;

    public ModifiedFile(ModifiedType modifiedType, Path relativePath, Path workspace) {
        Objects.requireNonNull(relativePath, "Relative path must not be null");
        PathVerifier.requireAbsolute(workspace, "Workspace");
        PathVerifier.requireRelative(relativePath, "File");

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
    public int compareTo(ModifiedFile o) {
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
        if(backslash)
            return modifiedType + " \\" + getRelativePath().toString().replace("/", "\\");
        return modifiedType + " /" + getRelativePath().toString().replace("\\", "/");
    }

    private static final long serialVersionUID = 1L;

}
