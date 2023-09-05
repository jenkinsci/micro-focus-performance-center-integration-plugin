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

import java.io.IOException;

public interface ICompressor {

    /**
     * Creates a compressed archive from directory and stores it in byte array.
     *
     * @param directoryPath path to source folder (which will be compressedped)
     * @return byte array containing target compressed archive
     * @throws IOException
     */
    byte[] compressDirectoryToByteArray(String directoryPath) throws IOException;

    /**
     * Creates a compressed archive from directory and stores it in byte array.
     *
     * @param directoryPath   path to source folder (which will be compressedped)
     * @param compressComment compressed archive comment (will be displayed e.g. in WinRAR)
     * @return byte array containing target compressed archive
     * @throws IOException
     */
    byte[] compressDirectoryToByteArray(String directoryPath, String compressComment) throws IOException;

    /**
     * Creates a compressed archive from directory and stores it in file.
     *
     * @param directoryPath          path to source folder (which will be compressped)
     * @param targetCompressFilePath path to target compressed archive
     * @param overwrite              if true, overwrite existing compressed archive, otherwise - throw exception if archive exists
     * @throws IOException
     */
    void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite) throws IOException;

    /**
     * Creates a compress archive from directory and stores it in file.
     *
     * @param directoryPath          path to source folder (which will be compressed)
     * @param targetCompressFilePath path to target compressed archive
     * @param overwrite              if true, overwrite existing compressed archive,
     *                               otherwise - throw exception if archive exists
     * @param compressComment        compressed archive comment (will be displayed e.g. in WinRAR)
     * @throws IOException
     */
    void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite, String compressComment) throws IOException;

}
