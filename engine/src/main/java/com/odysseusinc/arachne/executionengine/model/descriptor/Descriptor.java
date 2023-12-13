package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Descriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private List<String> osLibraries = new ArrayList<>();
    @JsonProperty
    private List<ExecutionRuntime> executionRuntimes = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getOsLibraries() {
        return osLibraries;
    }

    public void setOsLibraries(List<String> osLibraries) {
        this.osLibraries = osLibraries;
    }

    public List<ExecutionRuntime> getExecutionRuntimes() {
        return executionRuntimes;
    }

    public void setExecutionRuntimes(List<ExecutionRuntime> executionRuntimes) {
        this.executionRuntimes = executionRuntimes;
    }
}
