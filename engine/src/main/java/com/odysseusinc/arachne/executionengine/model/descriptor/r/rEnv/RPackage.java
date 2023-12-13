package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;

import static com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackage.SOURCE_GITHUB;
import static com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackage.SOURCE_REPOSITORY;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "Source")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RPackageRepository.class, name = SOURCE_REPOSITORY),
        @JsonSubTypes.Type(value = RPackageGitHub.class, name = SOURCE_GITHUB)
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class RPackage {
    static final String SOURCE_REPOSITORY = "Repository";
    static final String SOURCE_GITHUB = "GitHub";

    @JsonProperty("Package")
    private String name;
    @JsonProperty("Version")
    private String version;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public abstract RDependencySourceType getDependencySourceType();
}
