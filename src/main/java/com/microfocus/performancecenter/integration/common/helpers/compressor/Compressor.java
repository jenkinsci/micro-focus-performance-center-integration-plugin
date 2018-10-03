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

package com.microfocus.performancecenter.integration.common.helpers.compressor;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Simple utility class which helps to create compress archives.
 */


public class Compressor implements ICompressor {

    @Override
    public void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite) throws IOException {
        compressDirectoryToFile(directoryPath, targetCompressFilePath, overwrite, null);
    }

    @Override
    public void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite, String compressComment) throws IOException {
        File targetCompressFile = new File(targetCompressFilePath);

        if (!overwrite) {
            requireToBeNonExisting(targetCompressFile);
        }

        try (FileOutputStream fos = new FileOutputStream(targetCompressFile, false)) {
            compress(fos, directoryPath, compressComment);
        }
    }

    @Override
    public byte[] compressDirectoryToByteArray(String directoryPath) throws IOException {
        return compressDirectoryToByteArray(directoryPath, null);
    }

    @Override
    public byte[] compressDirectoryToByteArray(String directoryPath, String compressComment) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            compress(baos, directoryPath, compressComment);
            return baos.toByteArray();
        }
    }

    protected void compress(OutputStream target, String directoryPath, String compressComment) throws IOException {
        File directoryFile = requireToBeDirectory(new File(directoryPath));
        try (BufferedOutputStream bos = new BufferedOutputStream(target);
             ZipOutputStream zos = new ZipOutputStream(bos, Charset.forName("UTF-8"))) {

            putCompressEntriesForDirectory(zos, directoryFile, directoryFile.getAbsolutePath().length() + 1);
            zos.closeEntry();
            if (compressComment != null) {
                zos.setComment(compressComment);
            }
        }
    }

    protected void putCompressEntriesForDirectory(ZipOutputStream target, File directory, int rootPathLength) throws IOException {
        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                putCompressEntriesForDirectory(target, f, rootPathLength);
            } else {
                putCompressEntriesForFile(target, f, rootPathLength);
            }
        }
    }

    protected void putCompressEntriesForFile(ZipOutputStream target, File file, int rootPathLength) throws IOException {
        String path = file.getAbsolutePath();
        ZipEntry ze = new ZipEntry(path.substring(rootPathLength));
        target.putNextEntry(ze);
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            byte[] buffer = new byte[2048];
            int count;
            while ((count = bis.read(buffer)) != -1) {
                target.write(buffer, 0, count);
            }
        }
    }

    private static File requireToBeNonExisting(File file) {
        if (file.exists()) {
            throw new IllegalArgumentException(file + " already exists!");
        }

        return file;
    }

    private static File requireToBeDirectory(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(file + " does not exist!");
        }

        if (!file.isDirectory()) {
            throw new IllegalArgumentException(file + " is not a directory!");
        }

        return file;
    }
}
