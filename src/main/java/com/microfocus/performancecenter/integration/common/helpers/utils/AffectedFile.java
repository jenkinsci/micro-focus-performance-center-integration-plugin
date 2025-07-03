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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class AffectedFile implements Comparable<AffectedFile> {
    @NonNull
    private final Path fullPath;

    @NonNull
    private final Path relativePath;

    @NonNull
    private final String testName;

    public AffectedFile(Path fileFullPath, Path workspace) {
        this.fullPath = Objects.requireNonNull(fileFullPath, "fileFullPath must not be null");
        PathVerifier.requireAbsolute(fileFullPath, "FullPath");
        PathVerifier.requireAbsolute(workspace, "Workspace");

        this.relativePath = workspace.relativize(Helper.getParent(fileFullPath));

        // Ensure testName is not null by providing a default value if fileFullPath.getFileName() is null
        Path fileName = fileFullPath.getFileName();
        this.testName = (fileName != null)
                ? FilenameUtils.removeExtension(fileName.toString())
                : "UnknownTestName"; // You can replace "defaultTestName" with an appropriate fallback value
    }

    private static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public String getSubjectPath() {
        return relativePath.toString().replace('\\', File.separatorChar).replace('/', File.separatorChar);
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "FB doesn't seem to understand Objects.requireNonNull")
    public String getTestName(Path fileFullPath) {
        return fileFullPath.getFileName().toString();
    }

    @Override
    public int compareTo(AffectedFile o) {
        String FullTestName = relativePath.toString().concat(File.separator).concat(testName);
        String oFullTestName = o.relativePath.toString().concat(File.separator).concat(o.getTestName());
        return FullTestName.compareTo(oFullTestName);
    }

    @Override
    public String toString() {
        // we override the default relative representation of the root folder itself
        // for the sake of readability:
        String normalizedRelativePath = File.separator
                .concat(relativePath.toString().replace('/', File.separatorChar).replace('\\', File.separatorChar));
        if (!normalizedRelativePath.endsWith(File.separator)) {
            normalizedRelativePath = normalizedRelativePath.concat(File.separator);
        }
        return normalizedRelativePath.concat(testName);
    }

    public String getTestContent() {
        try {
            String content = readFile(this.fullPath.toString(), Charset.defaultCharset());
            if (content != null && !content.isEmpty())
                return content;
        } catch (IOException e) {
        }
        return "";
    }

    @NonNull
    public Path getFullPath() {
        return this.fullPath;
    }

    @NonNull
    public Path getRelativePath() {
        return this.relativePath;
    }

    @NonNull
    public String getTestName() {
        return this.testName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AffectedFile)) return false;
        final AffectedFile other = (AffectedFile) o;
        if (!this.getFullPath().equals(other.getFullPath())) return false;
        if (!this.getRelativePath().equals(other.getRelativePath())) return false;
        if (!this.getTestName().equals(other.getTestName())) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = PRIME + this.getFullPath().hashCode();
        result = result * PRIME + this.getRelativePath().hashCode();
        result = result * PRIME + this.getTestName().hashCode();
        return result;
    }
}

