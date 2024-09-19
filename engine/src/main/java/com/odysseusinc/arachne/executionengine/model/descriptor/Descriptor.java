package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Descriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private List<String> osLibraries;
    @JsonProperty
    private List<ExecutionRuntime> executionRuntimes;
}
