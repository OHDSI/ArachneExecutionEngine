package com.odysseusinc.arachne.executionengine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.execution_engine_common.util.CommonFileUtils;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.ExecutionRuntimeHelper;
import com.odysseusinc.arachne.executionengine.model.descriptor.DefaultDescriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DescriptorServiceImpl implements DescriptorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorServiceImpl.class);

    private static final String DESCRIPTOR_PREFIX = "descriptor";

    private RIsolatedRuntimeProperties rIsolatedRuntimeProps;

    private DescriptorBundle defaultDescriptorBundle;

    public DescriptorServiceImpl(RIsolatedRuntimeProperties rIsolatedRuntimeProps) {
        this.rIsolatedRuntimeProps = rIsolatedRuntimeProps;
        this.defaultDescriptorBundle = new DescriptorBundle(rIsolatedRuntimeProps.getArchive(), new DefaultDescriptor());
    }

    @Override
    public List<Descriptor> getDescriptors() {
        List<Descriptor> descriptors = new ArrayList<>();
        if (rIsolatedRuntimeProps.getArchiveFolder() != null) {
            File archiveFolder = new File(rIsolatedRuntimeProps.getArchiveFolder());
            if (archiveFolder.isDirectory()) {
                for (File file : archiveFolder.listFiles()) {
                    if (file.getName().startsWith(DESCRIPTOR_PREFIX)) {
                        try {
                            if (file.isFile()) {
                                InputStream is = new FileInputStream(file);
                                ObjectMapper mapper = new ObjectMapper();
                                Descriptor descriptor = mapper.readValue(is, Descriptor.class);
                                descriptors.add(descriptor);
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error getting descriptor from file: {}", file.getName());
                        }
                    }
                }
            }
        }
        return descriptors;
    }

    @Override
    public DescriptorBundle getDescriptorBundle(File file, Long id, String requestedDescriptorId) {
        DescriptorBundle descriptorBundle = defaultDescriptorBundle;
        if (rIsolatedRuntimeProps.getArchiveFolder() != null) {
            List<Descriptor> availableDescriptors = getDescriptors();
            if (!availableDescriptors.isEmpty()) {
                if (!StringUtils.isEmpty(requestedDescriptorId)) {
                    descriptorBundle = availableDescriptors.stream()
                            .filter(descriptor -> descriptor.getId().equals(requestedDescriptorId))
                            .map(descriptor -> {
                                String descriptorPath = getDescriptorPath(descriptor);
                                return new DescriptorBundle(descriptorPath, descriptor);
                            })
                            .findFirst()
                            .orElseGet(getDescriptorBundle(file, id, availableDescriptors));
                } else {
                    descriptorBundle = getDescriptorBundle(file, id, availableDescriptors).get();
                }
            }
        }
        return descriptorBundle;
    }

    private Supplier<DescriptorBundle> getDescriptorBundle(File file, Long id, List<Descriptor> availableDescriptors) {
        return () -> {
            DescriptorBundle descriptorBundle = defaultDescriptorBundle;
            File temporaryDir = com.google.common.io.Files.createTempDir();
            try {
                extractFiles(file, temporaryDir);
                List<File> files = Arrays.asList(temporaryDir.listFiles());
                Set<ExecutionRuntime> executionRuntimeSet = ExecutionRuntimeHelper.getRuntimes(files);
                Descriptor descriptor = new Descriptor();
                descriptor.getExecutionRuntimes().addAll(executionRuntimeSet);

                List<Descriptor> matchedDescriptors = availableDescriptors.stream()
                        .filter(availableDescriptor -> availableDescriptor.matches(descriptor))
                        .collect(Collectors.toList());

                if (matchedDescriptors.size() > 0) {
                    Descriptor matchedDescriptor = matchedDescriptors.get(0);
                    String descriptorPath = getDescriptorPath(matchedDescriptor);
                    File bundle = new File(descriptorPath);
                    if (bundle.isFile()) {
                        LOGGER.info("Found descriptor '{}' for analysis '{}' with bundle name '{}' and path '{}'",
                                matchedDescriptor.getLabel(), id, matchedDescriptor.getBundleName(), descriptorPath);
                        descriptorBundle = new DescriptorBundle(descriptorPath, matchedDescriptor);
                    }
                }
            } finally {
                temporaryDir.delete();
            }
            LOGGER.info("No bundle descriptor found for analysis {}. Using default", id);
            return descriptorBundle;
        };
    }

    private String getDescriptorPath(Descriptor descriptor) {
        return rIsolatedRuntimeProps.getArchiveFolder() + descriptor.getBundleName();
    }

    private void extractFiles(File parentFolder, File tempFolder) {
        for (File file : parentFolder.listFiles()) {
            try {
                CommonFileUtils.unzipFiles(file, tempFolder);
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
