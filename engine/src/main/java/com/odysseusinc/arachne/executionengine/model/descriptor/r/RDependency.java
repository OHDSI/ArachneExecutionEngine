package com.odysseusinc.arachne.executionengine.model.descriptor.r;

import java.util.List;
import java.util.Objects;

public class RDependency {
    private String name;
    private String version;
    private String owner;
    private RDependencySourceType dependencySourceType;
    private List<String> preInstallScripts;
    private List<String> postInstallScripts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public RDependencySourceType getDependencySourceType() {
        return dependencySourceType;
    }

    public void setDependencySourceType(RDependencySourceType dependencySourceType) {
        this.dependencySourceType = dependencySourceType;
    }

    public List<String> getPreInstallScripts() {
        return preInstallScripts;
    }

    public void setPreInstallScripts(List<String> preInstallScripts) {
        this.preInstallScripts = preInstallScripts;
    }

    public List<String> getPostInstallScripts() {
        return postInstallScripts;
    }

    public void setPostInstallScripts(List<String> postInstallScripts) {
        this.postInstallScripts = postInstallScripts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RDependency that = (RDependency) o;
        return name.equals(that.name) && version.equals(that.version) && dependencySourceType == that.dependencySourceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, dependencySourceType);
    }
}
