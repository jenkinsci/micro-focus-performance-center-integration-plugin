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

import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;
import hudson.model.TaskListener;

import java.io.PrintStream;

public class LogHelper {

    public static final String DEFAULT_PATTERN = "E yyyy MMM dd 'at' HH:mm:ss.SSS a zzz";

    private LogHelper() {
    }

    public static void log(TaskListener listener, String format, boolean addDate, Object... args) {
        if (listener == null || listener.equals(TaskListener.NULL)) {
            return;
        }

        try {
            String formatted = (format != null) ? format : "";

            // Count placeholders and compare with the number of arguments
            int expectedArgs = countPlaceholders(format);
            if (args.length < expectedArgs) {
                // Print mismatch details to the logger
                PrintStream ps = listener.getLogger();
                ps.println("Mismatched number of format specifiers and arguments." +
                        System.lineSeparator() +
                        "Format: " + formatted +
                        System.lineSeparator() +
                        "Expected args: " + expectedArgs +
                        System.lineSeparator() +
                        "Provided args length: " + args.length);

                // Optionally, print the args themselves
                ps.println("Provided arguments:");
                for (Object arg : args) {
                    ps.println("  - " + arg);
                }

                return; // Return early if there is a mismatch
            }

            // Add date if required
            if (!formatted.isEmpty() && addDate) {
                DateFormatter dateFormatter = new DateFormatter(DEFAULT_PATTERN);
                formatted = String.format("%s - ", dateFormatter.getDate()) + formatted;
            }

            // Safely format and log the message
            if (!formatted.isEmpty()) {
                PrintStream ps = listener.getLogger();
                ps.println(String.format(formatted, args));
            } else {
                PrintStream ps = listener.getLogger();
                ps.println("");
            }

        } catch (Exception ex) {
            // Catch any exception and print it
            ex.printStackTrace();
            PrintStream ps = listener.getLogger();
            ps.println("Error occurred while formatting log message: " + ex.getMessage());
        }
    }



    public static void logStackTrace(TaskListener listener, ConfigureSystemSection configureSystemSection, Throwable t) {
        if (listener == null || listener.equals(TaskListener.NULL)) {
            return;
        }
        logStackTrace(listener, configureSystemSection.getDebug(), t);
    }

    public static void logStackTrace(TaskListener listener, boolean debug, Throwable t) {
        if (listener == null || listener.equals(TaskListener.NULL))
            return;

        if (debug) {
            PrintStream ps = listener.getLogger();
            ps.print("  Stack Trace: ");
            t.printStackTrace(ps);
        }
    }

    public static void printDescription(TaskListener listener, String description) {
        String line = String.format("%" + (description.length() + 6) + "s", "").replace(' ', '=');
        String border = String.format("%s%" + description.length() + "s%s", "=  ", "", "  =");
        boolean addDate = false;

        log(listener, line, addDate);
        log(listener, border, addDate);
        log(listener, "=  %s  =", addDate, description);
        log(listener, border, addDate);
        log(listener, line, addDate);
    }

    private static int countPlaceholders(String format) {
        int count = 0;
        if (format != null) {
            // Regular expression to match format specifiers like %s, %d, etc.
            count = format.split("%[a-zA-Z]").length - 1;
        }
        return count;
    }

}
