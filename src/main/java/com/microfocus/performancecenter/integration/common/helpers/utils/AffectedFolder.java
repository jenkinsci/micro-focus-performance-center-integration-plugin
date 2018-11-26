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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Value
@EqualsAndHashCode(of = "relativePath")
@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "lombok generated sources")
public class AffectedFolder implements Comparable<AffectedFolder> {

    @Nonnull
    private final Path fullPath;

    @Nonnull
    private final Path relativePath;

    public AffectedFolder(Path folderFullPath, Path workspace) {
        this.fullPath = Objects.requireNonNull(folderFullPath);
        PathVerifier.requireAbsolute(folderFullPath, "Folder");
        PathVerifier.requireAbsolute(workspace, "Workspace");

        this.relativePath = workspace.relativize(folderFullPath);
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "FB doesn't seem to understand Objects.requireNonNull")
    public String getSubjectPath() {
        String subjectRoot = "Subject";
        String scriptPath =  subjectRoot + "\\" + relativePath.toString().replace(File.separatorChar, '\\');
        Path script = Paths.get(scriptPath);
        if (script.getParent().toString().equals(subjectRoot))
            return scriptPath;
        else
            return script.getParent().toString();
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
        if(backslash)
            return "\\" + relativePath.toString().replace("/", "\\");
        return "//" + relativePath.toString().replace("\\", "//");
    }
}
