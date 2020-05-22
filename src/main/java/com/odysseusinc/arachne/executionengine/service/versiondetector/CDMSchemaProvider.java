package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.odysseusinc.arachne.executionengine.exceptions.ExecutionEngineRuntimeException;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class CDMSchemaProvider {

    private static final TypeReference<TreeMap<String, Map<String, List<String>>>> TREE_MAP_TYPE_REFERENCE = new TypeReference<TreeMap<String, Map<String, List<String>>>>() {
    };

    public static String MANDATORY_COLUMNS_KEY = "mandatory_columns";
    public static String OPTIONAL_COLUMNS_KEY = "optional_columns";

    private final Cache<String, TreeMap<String, Map<String, List<String>>>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    public Map<String, List<String>> loadMandatorySchemaJson(String resource) {

        return getFullJson(resource).get(MANDATORY_COLUMNS_KEY);
    }

    public Map<String, List<String>> loadOptionalSchemaJson(String resource) {

        return getFullJson(resource).getOrDefault(OPTIONAL_COLUMNS_KEY, Collections.emptyMap());
    }

    private TreeMap<String, Map<String, List<String>>> getFullJson(String resource) {

        try {
            return cache.get(resource, () -> loadResource(resource));
        } catch (ExecutionException e) {
            throw new ExecutionEngineRuntimeException(e);
        }
    }

    private TreeMap<String, Map<String, List<String>>> loadResource(String resource) {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(resource))) {
            return mapper.readValue(reader, TREE_MAP_TYPE_REFERENCE);
        } catch (Exception e) {
            throw new ExecutionEngineRuntimeException("Cannot load resource: " + resource, e);
        }
    }
}
