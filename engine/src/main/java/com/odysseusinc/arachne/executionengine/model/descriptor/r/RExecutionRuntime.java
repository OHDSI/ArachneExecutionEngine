package com.odysseusinc.arachne.executionengine.model.descriptor.r;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.execution_engine_common.descriptor.RuntimeType;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackage;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.RPackageGitHub;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RExecutionRuntime implements ExecutionRuntime {
    @JsonProperty
    private String version;
    @JsonProperty
    private List<RDependency> dependencies;

    public String getMismatches(ExecutionRuntime requested) {
        if (requested instanceof RExecutionRuntime) {
            RExecutionRuntime other = (RExecutionRuntime) requested;
            String differences = Stream.concat(
                    calcVersionMismatch(other), calcDependencyMismatch(other)
            ).collect(Collectors.joining(", "));

            return (differences.length() > 0) ? ("Not matched [" + other + "] to [" + this + "]: " + differences) : null;
        } else {
            return "Not matched [" + requested + "] to [" + this + "]: type mismatch";
        }
    }

    private Stream<String> calcVersionMismatch(RExecutionRuntime other) {
        return version.equals(other.version) ? Stream.of() : Stream.of(
                String.format("Required R version '%s' does not match existing R version '%s'", other.version, version)
        );
    }

    private Stream<String> calcDependencyMismatch(RExecutionRuntime other) {
        return other.dependencies.stream().filter(dependency ->
                !dependencies.contains(dependency)
        ).map(dependency ->
                String.format("Missing dependency %s:%s via %s", dependency.getName(), dependency.getVersion(), dependency.getDependencySourceType())
        );
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

    public List<RDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<RDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public String toString() {
        return getType().name() + ":" + getVersion();
    }
}
