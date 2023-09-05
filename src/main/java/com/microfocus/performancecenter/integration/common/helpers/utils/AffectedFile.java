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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

//@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "lombok generated sources")
public final class AffectedFile implements Comparable<AffectedFile> {
    @Nonnull
    private final Path fullPath;

    @Nonnull
    private final Path relativePath;

    @Nonnull
    private final String testName;

    public AffectedFile(Path fileFullPath, Path workspace) {
        this.fullPath = Objects.requireNonNull(fileFullPath);
        PathVerifier.requireAbsolute(fileFullPath, "FullPath");
        PathVerifier.requireAbsolute(workspace, "Workspace");

        this.relativePath = workspace.relativize(Helper.getParent(fileFullPath));
        this.testName = FilenameUtils.removeExtension(fileFullPath.getFileName().toString());
    }

    private static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "FB doesn't seem to understand Objects.requireNonNull")
    public String getSubjectPath() {
        return relativePath.toString().replace(File.separatorChar, '\\').replace("/", "\\");
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "FB doesn't seem to understand Objects.requireNonNull")
    public String getTestName(Path fileFullPath) {
        return fileFullPath.getFileName().toString();
    }

    @Override
    public int compareTo(AffectedFile o) {
        String FullTestName = relativePath.toString().concat("\\").concat(testName);
        String oFullTestName = o.relativePath.toString().concat("\\").concat(o.getTestName());
        return FullTestName.compareTo(oFullTestName);
    }

    @Override
    public String toString() {
        // we override the default relative representation of the root folder itself
        // for the sake of readability:
        return "/".concat(relativePath.toString()).concat("/").concat(testName);
    }

    public String toString(boolean backslash) {
        if (backslash)
            return "\\" + relativePath.toString().replace("/", "\\").concat("\\").concat(testName);
        return "//" + relativePath.toString().replace("\\", "//").concat("/").concat(testName);
    }

    public String getTestContent() {
        if (fullPath == null)
            return "";
        try {
            String content = readFile(this.fullPath.toString(), Charset.defaultCharset());
            if (content != null && !content.isEmpty())
                return content;
        } catch (IOException e) {
        }
        return "";
    }

    @Nonnull
    public Path getFullPath() {
        return this.fullPath;
    }

    @Nonnull
    public Path getRelativePath() {
        return this.relativePath;
    }

    @Nonnull
    public String getTestName() {
        return this.testName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AffectedFile)) return false;
        final AffectedFile other = (AffectedFile) o;
        final Object this$fullPath = this.getFullPath();
        final Object other$fullPath = other.getFullPath();
        if (this$fullPath == null ? other$fullPath != null : !this$fullPath.equals(other$fullPath)) return false;
        final Object this$relativePath = this.getRelativePath();
        final Object other$relativePath = other.getRelativePath();
        if (this$relativePath == null ? other$relativePath != null : !this$relativePath.equals(other$relativePath))
            return false;
        final Object this$testName = this.getTestName();
        final Object other$testName = other.getTestName();
        if (this$testName == null ? other$testName != null : !this$testName.equals(other$testName)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $fullPath = this.getFullPath();
        result = result * PRIME + ($fullPath == null ? 43 : $fullPath.hashCode());
        final Object $relativePath = this.getRelativePath();
        result = result * PRIME + ($relativePath == null ? 43 : $relativePath.hashCode());
        final Object $testName = this.getTestName();
        result = result * PRIME + ($testName == null ? 43 : $testName.hashCode());
        return result;
    }
}

