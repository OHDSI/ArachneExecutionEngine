package com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RRepository {
    @JsonProperty("Name")
    private String name;
    @JsonProperty("URL")
    private String url;
}
