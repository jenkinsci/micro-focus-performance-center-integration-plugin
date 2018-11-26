package com.microfocus.performancecenter.integration.common.helpers.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Value
@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "lombok generated sources")
public class AffectedFile implements Comparable<AffectedFile>{
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

        this.relativePath = workspace.relativize(fileFullPath.getParent());
        this.testName = FilenameUtils.removeExtension(fileFullPath.getFileName().toString());
    }

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "FB doesn't seem to understand Objects.requireNonNull")
    public String getSubjectPath() {
        return  relativePath.toString().replace(File.separatorChar, '\\');
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
        if(backslash)
            return "\\" + relativePath.toString().replace("/", "\\").concat("\\").concat(testName);
        return "//" + relativePath.toString().replace("\\", "//").concat("/").concat(testName);
    }

    public String getTestContent() {
        if (fullPath == null)
            return "";
        try {
            String content = readFile(this.fullPath.toString(), Charset.defaultCharset());
            if (content != null || !content.isEmpty())
                return content;
        } catch (IOException e) {
        }
        return "";
    }

    private static String readFile(String path, Charset encoding)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}

