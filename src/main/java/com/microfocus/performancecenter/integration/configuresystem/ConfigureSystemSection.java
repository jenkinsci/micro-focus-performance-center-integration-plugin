package com.microfocus.performancecenter.integration.configuresystem;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import lombok.Getter;
import lombok.Setter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import java.io.Serializable;

import java.io.Serializable;

@Getter
@Setter
@Extension
public class ConfigureSystemSection extends GlobalConfiguration implements Serializable {

    private boolean debug;

    public boolean getDebug() { return debug;}

    public ConfigureSystemSection() {
        load();
    }

    public static ConfigureSystemSection get() {
        return GlobalConfiguration.all().get(ConfigureSystemSection.class);
    }

    private final static long serialVersionUID = 1L;

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return super.configure(req, json);
    }


}
