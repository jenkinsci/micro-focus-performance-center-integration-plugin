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

import java.io.IOException;

public interface ICompressor {

    /**
     * Creates a compressed archive from directory and stores it in byte array.
     *
     * @param directoryPath path to source folder (which will be compressedped)
     *
     * @return byte array containing target compressed archive
     *
     * @throws IOException
     */
    byte[] compressDirectoryToByteArray(String directoryPath) throws IOException;

    /**
     * Creates a compressed archive from directory and stores it in byte array.
     *
     * @param directoryPath path to source folder (which will be compressedped)
     * @param compressComment    compressed archive comment (will be displayed e.g. in WinRAR)
     *
     * @return byte array containing target compressed archive
     *
     * @throws IOException
     */
    byte[] compressDirectoryToByteArray(String directoryPath, String compressComment) throws IOException;

    /**
     * Creates a compressed archive from directory and stores it in file.
     *
     * @param directoryPath     path to source folder (which will be compressped)
     * @param targetCompressFilePath path to target compressed archive
     * @param overwrite         if true, overwrite existing compressed archive, otherwise - throw exception if archive exists
     *
     * @throws IOException
     */
    void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite) throws IOException;

    /**
     * Creates a compress archive from directory and stores it in file.
     *
     * @param directoryPath     path to source folder (which will be compressed)
     * @param targetCompressFilePath path to target compressed archive
     * @param overwrite         if true, overwrite existing compressed archive,
     *                          otherwise - throw exception if archive exists
     * @param compressComment        compressed archive comment (will be displayed e.g. in WinRAR)
     *
     * @throws IOException
     */
    void compressDirectoryToFile(String directoryPath, String targetCompressFilePath, boolean overwrite, String compressComment) throws IOException;

}
