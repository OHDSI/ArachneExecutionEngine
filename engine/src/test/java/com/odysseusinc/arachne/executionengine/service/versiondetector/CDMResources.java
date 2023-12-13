package com.odysseusinc.arachne.executionengine.service.versiondetector;

public enum CDMResources {
    V4_SCHEMA("V4_0", "/cdm/v4/cdm_V4_0.json"),
    V5_COMMONS_SCHEMA(null, "/cdm/v5/cdm_commons.json"),
    V5_0_SCHEMA_DIFF("V5_0", "/cdm/v5/diff_V5_0.json"),
    V5_0_1_SCHEMA_DIFF("V5_0_1", "/cdm/v5/diff_V5_0_1.json"),
    V5_1_SCHEMA_DIFF("V5_1", "/cdm/v5/diff_V5_1.json"),
    V5_2_SCHEMA_DIFF("V5_2", "/cdm/v5/diff_V5_2.json"),
    V5_3_SCHEMA_DIFF("V5_3", "/cdm/v5/diff_V5_3.json"),
    V5_3_1_SCHEMA_DIFF("V5_3_1", "/cdm/v5/diff_V5_3_1.json"),
    V6_SCHEMA("V6_0", "/cdm/v6/cdm_V6_0.json");

    private final String versionDTO;
    private final String path;

    CDMResources(String versionDTO, String path) {

        this.versionDTO = versionDTO;
        this.path = path;
    }

    public String getVersionDTO() {

        return versionDTO;
    }

    public String getPath() {

        return path;
    }
}
