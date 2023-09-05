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

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter {

    public static final String DEFAULT_PATTERN = "E yyyy MMM dd 'at' HH:mm:ss.SSS a zzz";

    private SimpleDateFormat simpleDateFormat;
    private String pattern;
    private String date;

    public DateFormatter(String pattern) {
        this.pattern = pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
        simpleDateFormat = new SimpleDateFormat(this.pattern);
        renewDate();
    }

    public void renewDate() {
        try {
            date = simpleDateFormat.format(new Date());
        } catch (Exception ex) {
            this.pattern = DEFAULT_PATTERN;
            simpleDateFormat = new SimpleDateFormat(this.pattern);
            date = simpleDateFormat.format(new Date());
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern.isEmpty() ? DEFAULT_PATTERN : pattern;
        simpleDateFormat = new SimpleDateFormat(this.pattern);
    }

    public String getDate() {
        renewDate();
        return date;
    }

}
