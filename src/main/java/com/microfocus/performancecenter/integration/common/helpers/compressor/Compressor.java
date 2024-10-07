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

package com.microfocus.performancecenter.integration.common.helpers.compressor;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Simple utility class which helps to create compress archives.
 */


public class Compressor implements ICompressor {

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
                /*
                 * Create an 'directory entry' in the zip-file. It should not be neccessary according the 
                 * zip specifications but Loadrunner enterprise uploaded seems to strip folder in a script 
                 * if this is not here (eg. node_modules in a DevWeb script).
                 */
                String path = f.getAbsolutePath();
                /*
                 * Note: According to the zip specifications https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
                 * section 4.4.17.1 the path separator must be a forward slash.
                 */
                ZipEntry ze = new ZipEntry(path.substring(rootPathLength).replace("\\", "/") + "/");
                target.putNextEntry(ze);
                /*
                 * Now recurse into the directory structure and continue.
                 */
                putCompressEntriesForDirectory(target, f, rootPathLength);
            } else {
                putCompressEntriesForFile(target, f, rootPathLength);
            }
        }
    }

    protected void putCompressEntriesForFile(ZipOutputStream target, File file, int rootPathLength) throws IOException {
        String path = file.getAbsolutePath();
        /*
        * Note: According to the zip specifications https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
        * section 4.4.17.1 the path separator must be a forward slash.
        */
        ZipEntry ze = new ZipEntry(path.substring(rootPathLength).replace("\\", "/"));
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
}
