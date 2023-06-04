package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;

import java.io.File;
import java.util.List;

public interface DescriptorService {
    List<Descriptor> getDescriptors();

    DescriptorBundle getDescriptorBundle(File file, Long id, String requestedDescriptorId);
}
