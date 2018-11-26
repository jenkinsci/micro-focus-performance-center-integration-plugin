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

import hudson.model.TaskListener;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.microfocus.performancecenter.integration.configuresystem.ConfigureSystemSection;

import java.io.PrintStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogHelper {

    public static final String DEFAULT_PATTERN = "E yyyy MMM dd 'at' HH:mm:ss.SSS a zzz";

    public static void log(TaskListener listener, String format, boolean addDate, Object... args) {
        if (listener == null || listener.equals(TaskListener.NULL))
            return;
        String formatted = format;
        if(format != null && !format.isEmpty() && addDate) {
            DateFormatter dateFormatter = new DateFormatter(DEFAULT_PATTERN);
            formatted = String.format("%s - ", dateFormatter.getDate()) + format;
        }
        listener.getLogger().println(String.format(formatted, args));
    }


    public static void logStackTrace(TaskListener listener, ConfigureSystemSection configureSystemSection, Throwable t) {
        if (listener==null || listener.equals(TaskListener.NULL)) {
            return;
        }
        logStackTrace(listener, configureSystemSection.getDebug(), t);
    }

    public static void logStackTrace(TaskListener listener, boolean debug, Throwable t) {
        if (listener == null || listener.equals(TaskListener.NULL))
            return;

        if(debug) {
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

}
