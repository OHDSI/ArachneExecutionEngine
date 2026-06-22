package com.odysseusinc.arachne.execution_engine_common.proxy;

public class ProxySettings {

    private ProxyMode mode;
    private FixedInstanceSettings fixedInstanceSettings;
    private ElasticInstanceSettings elasticInstanceSettings;

    public ProxyMode getMode() {

        return mode;
    }

    public void setMode(ProxyMode mode) {

        this.mode = mode;
    }

    public FixedInstanceSettings getFixedInstanceSettings() {

        return fixedInstanceSettings;
    }

    public void setFixedInstanceSettings(FixedInstanceSettings fixedInstanceSettings) {

        this.fixedInstanceSettings = fixedInstanceSettings;
    }

    public ElasticInstanceSettings getElasticInstanceSettings() {

        return elasticInstanceSettings;
    }

    public void setElasticInstanceSettings(ElasticInstanceSettings elasticInstanceSettings) {

        this.elasticInstanceSettings = elasticInstanceSettings;
    }
}
