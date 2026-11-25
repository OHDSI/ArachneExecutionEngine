package com.odysseusinc.arachne.execution_engine_common.proxy;

import java.util.List;

public class ElasticInstanceSettings {

    private InstanceSettings instanceSettings;
    private List<String> supportedAnalysisTypes;

    public InstanceSettings getInstanceSettings() {

        return instanceSettings;
    }

    public void setInstanceSettings(InstanceSettings instanceSettings) {

        this.instanceSettings = instanceSettings;
    }

    public List<String> getSupportedAnalysisTypes() {

        return supportedAnalysisTypes;
    }

    public void setSupportedAnalysisTypes(List<String> supportedAnalysisTypes) {

        this.supportedAnalysisTypes = supportedAnalysisTypes;
    }
}
