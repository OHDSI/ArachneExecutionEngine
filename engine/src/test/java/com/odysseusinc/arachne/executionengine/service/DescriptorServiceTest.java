package com.odysseusinc.arachne.executionengine.service;

import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.impl.DescriptorServiceImpl;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DescriptorServiceTest {
    private final static String DEFAULT = "Default";
    private final static Path ENVS = new File("src/test/resources/environments").toPath();

    @Test
    public void requestedHades() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, "hades_0.0.1");
        Assertions.assertEquals("hades_0.0.1", bundle.getDescriptor().getId());
        Assertions.assertNotNull(bundle.getDescriptor());
        Assertions.assertFalse(bundle.getDescriptor().getExecutionRuntimes().isEmpty(), "Execution Runtimes is empty");
    }

    @Test
    public void requestedUnknownFallbackToMatching() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS,  true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, "foo");
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void requestedUnknownFallbackToMatchingFails() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, "descriptor_base.json", true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-mismatch"), 1L, "foo");
        Assertions.assertEquals(DEFAULT, bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategus() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusNested() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-nested"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusInZip() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-zip"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingDisabledStrategus() {
        // DependencyMatching flag controls logging, not matching!
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, true);
        DescriptorBundle bundle = subj.getDescriptorBundle(analysisFolder("strategus-match"), 1L, null);
        Assertions.assertEquals("descriptor_strategus_0.0.6", bundle.getDescriptor().getId());
    }

    @Test
    public void matchingEnabledStrategusMismatch() {
        DescriptorServiceImpl subj = new DescriptorServiceImpl(ENVS, "descriptor_base.json", true);
        Assertions.assertEquals(DEFAULT, subj.getDescriptorBundle(analysisFolder("strategus-mismatch"), 1L, null).getDescriptor().getId());
    }

    private static File analysisFolder(String subfolder) {
        return new File("src/test/resources/analysis/" + subfolder);
    }
}
