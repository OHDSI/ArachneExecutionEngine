package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RDependencySourceType;

public class RPackageRepository extends RPackage {
    @JsonProperty("Repository")
    private String repository;

    public String getRepository() {
        return repository;
    }

    @Override
    public RDependencySourceType getDependencySourceType() {
        return RDependencySourceType.CRAN;
    }
}
