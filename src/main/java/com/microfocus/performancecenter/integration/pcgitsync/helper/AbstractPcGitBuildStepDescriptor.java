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


package com.microfocus.performancecenter.integration.pcgitsync.helper;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

public abstract class AbstractPcGitBuildStepDescriptor extends BuildStepDescriptor<Builder> {

    protected AbstractPcGitBuildStepDescriptor() {
        load();
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> type) {
        // Indicates that this builder can be used with all kinds of project types 
        return true;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        req.bindJSON(this, formData);
        save();
        return super.configure(req, formData);
    }

}
