package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.DescriptorDTO;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.DescriptorConverter;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependency;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Descriptor {
    @JsonProperty
    private String id;
    @JsonProperty
    private String bundleName;
    @JsonProperty
    private String label;
    @JsonProperty
    private Set<String> osLibraries = new HashSet<>();
    @JsonProperty
    private Set<ExecutionRuntime> executionRuntimes = new HashSet<>();

    public boolean matches(Descriptor descriptor) {
        boolean result = descriptor.getExecutionRuntimes().stream()
                .map(otherRuntime -> executionRuntimes.stream()
                        .filter(executionRuntime -> executionRuntime.getRuntimeType().equals(otherRuntime.getRuntimeType()))
                        .anyMatch(executionRuntime -> executionRuntime.matches(otherRuntime))
                )
                .reduce(true, (a, b) -> a && b);
        return result;
    }

    public void saveToFile(String filename) throws IOException {
        File f = new File(filename);
        OutputStream is = new FileOutputStream(f);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(is, this);
    }

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

    public Set<String> getOsLibraries() {
        return osLibraries;
    }

    public void setOsLibraries(Set<String> osLibraries) {
        this.osLibraries = osLibraries;
    }

    public Set<ExecutionRuntime> getExecutionRuntimes() {
        return executionRuntimes;
    }

    public void setExecutionRuntimes(Set<ExecutionRuntime> executionRuntimes) {
        this.executionRuntimes = executionRuntimes;
    }
}
