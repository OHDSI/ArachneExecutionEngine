package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import java.io.File;
import java.util.List;
import java.util.Optional;

public interface DescriptorService {
    Optional<List<Descriptor>> getDescriptors();

    List<Descriptor> getDescriptors(String id);

    DescriptorBundle getDescriptorBundle(File file, Long analysisId, String requestedDescriptorId);
}
