package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.executionengine.model.descriptor.DefaultDescriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DescriptorServiceTest {
    private final static DescriptorBundle DEFAULT = new DescriptorBundle("default.tar.gz", new DefaultDescriptor());
    private final static Optional<Path> ENVS = Optional.of(new File("src/test/resources/environments").toPath());

    @Test
    public void requestedHades() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, "hades_0.0.1");
        Assertions.assertEquals("hades_0.0.1", bundle.getDescriptor().getId());
    }

    @Test
    public void requestedUnknownFallbackToMatching() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, "foo");
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void requestedUnknownFallbackToMatchingFails() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-mismatch"), 1L, "foo");
        Assertions.assertEquals(DEFAULT, bundle);
    }

    @Test
    public void matchingEnabledStrategus() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusNested() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-nested"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusInZip() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-zip"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingDisabledStrategus() {
        // DependencyMatching flag controls logging, not matching!
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusMismatch() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(DEFAULT, ENVS, true);
        Assertions.assertEquals(DEFAULT, subj.getDescriptorBundle(analysisFolder("strategus-mismatch"), 1L, null));
    }

    private static File analysisFolder(String subfolder) {
        return new File("src/test/resources/analysis/" + subfolder);
    }
}
