package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;

public class RPackageGitHub extends RPackage {
    @JsonProperty("RemoteType")
    private String remoteType;
    @JsonProperty("RemoteHost")
    private String remoteHost;
    @JsonProperty("RemoteRepo")
    private String remoteRepo;
    @JsonProperty("RemoteUsername")
    private String remoteUsername;
    @JsonProperty("RemoteRef")
    private String remoteRef;

    public String getRemoteType() {
        return remoteType;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public String getRemoteRepo() {
        return remoteRepo;
    }

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public String getRemoteRef() {
        return remoteRef;
    }

    public String getVersion() {
        return remoteRef;
    }

    @Override
    public RDependencySourceType getDependencySourceType() {
        return RDependencySourceType.GITHUB;
    }
}
