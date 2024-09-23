package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DockerDescriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private List<String> osLibraries;

    public String getId() {
        return this.id;
    }

    public String getBundleName() {
        return this.bundleName;
    }

    public String getLabel() {
        return this.label;
    }

    public java.util.List<String> getOsLibraries() {
        return this.osLibraries;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOsLibraries(java.util.List<String> osLibraries) {
        this.osLibraries = osLibraries;
    }
}
