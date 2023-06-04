package com.odysseusinc.arachne.executionengine.model.descriptor.r;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackage;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackageGitHub;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RExecutionRuntime implements ExecutionRuntime {
    @JsonProperty
    private String version;
    @JsonProperty
    private List<RDependency> dependencies = new ArrayList<>();

    public boolean matches(ExecutionRuntime otherRuntime) {
        if (!(otherRuntime instanceof RExecutionRuntime))
            return false;
        RExecutionRuntime rExecutionRuntime = (RExecutionRuntime) otherRuntime;
        return this.version.equals(rExecutionRuntime.version)
                && this.dependencies.containsAll(rExecutionRuntime.dependencies);
    }

    public static RExecutionRuntime fromREnvLock(REnvLock rEnvLock) {
        RExecutionRuntime runtime = new RExecutionRuntime();
        runtime.version = rEnvLock.getrEnv().getVersion();
        runtime.dependencies = rEnvLock.getPackageMap().entrySet().stream()
                .map(entry -> {
                    RDependency dependency = new RDependency();
                    dependency.setName(entry.getKey());

                    RPackage rPackage = entry.getValue();
                    dependency.setDependencySourceType(rPackage.getDependencySourceType());
                    dependency.setVersion(rPackage.getVersion());

                    if (rPackage instanceof RPackageGitHub) {
                        dependency.setOwner(((RPackageGitHub) rPackage).getRemoteUsername());
                    }
                    return dependency;
                })
                .collect(Collectors.toList());
        return runtime;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.R;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public List<String> createInstallScripts() {
        throw new NotImplementedException();
    }

    public List<RDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<RDependency> dependencies) {
        this.dependencies = dependencies;
    }
}
