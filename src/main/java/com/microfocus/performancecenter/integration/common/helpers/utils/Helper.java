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

import com.microfocus.performancecenter.integration.common.helpers.constants.PcTestRunConstants;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Helper {

    public static Path getParent(Path path) {
        if (path.getParent() != null)
            return path.getParent();
        return Paths.get(getParent(path.toString()));
    }

    private static String getParent(String strPath) {
        char chrSeparatorBackward = '\\';
        char chrSeparatorForward = '/';
        String strPathToHandle = strPath;
        if (strPathToHandle == null || strPathToHandle.isEmpty() || !(strPathToHandle.indexOf(chrSeparatorBackward) != -1 || strPathToHandle.indexOf(chrSeparatorForward) != -1))
            return "";
        if (strPathToHandle.indexOf(chrSeparatorForward) != -1)
            strPathToHandle = strPathToHandle.replace(chrSeparatorForward, chrSeparatorBackward);
        if (strPathToHandle.endsWith(String.valueOf(chrSeparatorBackward)))
            strPathToHandle = strPathToHandle.replaceAll("\\$", "");

        int index = strPathToHandle.lastIndexOf(chrSeparatorBackward);
        strPathToHandle = strPathToHandle.substring(0, index);
        return strPathToHandle;
    }

    public static boolean isUsrScript(String strPath) {
        File dir = new File(strPath);
        if (dir == null || !dir.isDirectory()) {
            // Handle the case where the path is invalid or not a directory
            return false;
        }

        File[] foundFiles = dir.listFiles((dir1, filename) -> filename.toLowerCase().endsWith(PcTestRunConstants.USR_EXTENSION));

        // Check if foundFiles is null before accessing length
        return foundFiles != null && foundFiles.length > 0;
    }
}
