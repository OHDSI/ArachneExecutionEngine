package com.odysseusinc.arachne.executionengine.model.descriptor.r;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackage;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackageGitHub;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
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

    public List<String> getDiff(ExecutionRuntime otherRuntime) {
        if (!(otherRuntime instanceof RExecutionRuntime))
            return Collections.emptyList();
        List<String> differences = new ArrayList<>();
        RExecutionRuntime rExecutionRuntime = (RExecutionRuntime) otherRuntime;
        if (!this.version.equals(rExecutionRuntime.version)) {
            differences.add(getRuntimeVersionDiffString(rExecutionRuntime.version, this.version));
        }
        List<RDependency> dependencies = new ArrayList<>(rExecutionRuntime.dependencies);
        dependencies.removeAll(this.dependencies);
        dependencies.forEach(
                dependency -> differences.add(getDependencyAbsentString(dependency))
        );
        return differences;
    }

    private String getRuntimeVersionDiffString(String requiredVersion, String existingVersion) {
        return String.format("Required R version '%s' does not match existing R version '%s'",
                requiredVersion, existingVersion);
    }

    private String getDependencyAbsentString(RDependency dependency) {
        return String.format("Required dependency '%s:%s' is absent", dependency.getName(), dependency.getVersion());
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
    public RuntimeType getType() {
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
