package com.odysseusinc.arachne.executionengine.config.runtimeservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "runtimeservice.dist")
public class RIsolatedRuntimeProperties {

    private String[] runCmd;
    private String jailSh;
    private String cleanupSh;
    private String archive;

    public String[] getRunCmd() {
        return runCmd;
    }

    public void setRunCmd(String[] runCmd) {
        this.runCmd = runCmd;
    }

    public String getJailSh() {
        return jailSh;
    }

    public void setJailSh(String jailSh) {
        this.jailSh = jailSh;
    }

    public String getCleanupSh() {
        return cleanupSh;
    }

    public void setCleanupSh(String cleanupSh) {
        this.cleanupSh = cleanupSh;
    }

    public String getArchive() {
        return archive;
    }

    public void setArchive(String distArchive) {
        this.archive = distArchive;
    }
}
