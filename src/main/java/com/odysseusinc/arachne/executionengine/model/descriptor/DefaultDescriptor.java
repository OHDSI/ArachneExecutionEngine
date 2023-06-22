package com.odysseusinc.arachne.executionengine.model.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvLock;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public final class DefaultDescriptor extends Descriptor {
    private  static final String DEFAULT_DESCRIPTOR_LABEL = "Default descriptor";
    public  static final String DEFAULT_DESCRIPTOR_ID = "Default";

    @Override
    public String getBundleName() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getLabel() {
        return DEFAULT_DESCRIPTOR_LABEL;
    }

    @Override
    public String getId() {
        return DEFAULT_DESCRIPTOR_ID;
    }

    public boolean isDefaultDescriptor() {
        return true;
    }
}
