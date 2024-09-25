package com.odysseusinc.arachne.executionengine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odysseusinc.arachne.executionengine.config.runtimeservice.RIsolatedRuntimeProperties;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.DescriptorBundle;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.RExecutionRuntime;
import com.odysseusinc.arachne.executionengine.model.descriptor.r.rEnv.REnvParseStrategy;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import com.odysseusinc.arachne.executionengine.util.Streams;
import com.odysseusinc.arachne.executionengine.util.ZipInputSubStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class DescriptorServiceImpl implements DescriptorService {
    private static final REnvParseStrategy PARSE_STRATEGY = new REnvParseStrategy();
    private static final String DESCRIPTOR_PREFIX = "descriptor";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path archiveFolder;
    private final String defaultDescriptorFile;
    // TODO Consider replacing this flag with a dedicated logger
    private final boolean dependencyMatching;

    @Getter
    private volatile List<Descriptor> descriptors = Collections.emptyList();

    @Autowired
    public DescriptorServiceImpl(RIsolatedRuntimeProperties rIsolatedRuntimeProps) {
        this(
                new File(rIsolatedRuntimeProps.getArchiveFolder()).toPath(),
                rIsolatedRuntimeProps.getDefaultDescriptorFile(),
                rIsolatedRuntimeProps.isApplyRuntimeDependenciesComparisonLogic()
        );
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private void refresh() {
        List<Descriptor> old = descriptors;
        try {
            Optional<Descriptor> defaults = defaultDescriptor();
            List<Descriptor> found = listDescriptors();
            Stream<Descriptor> filtered = defaults.map(Descriptor::getId).map(def ->
                    found.stream().filter(descriptor -> !descriptor.getId().equals(def))
            ).orElseGet(found::stream);
            List<Descriptor> latest = Stream.concat(Streams.from(defaults), filtered).collect(Collectors.toList());
            if (old == null || old.size() != latest.size()) {
                log.info("Refreshed TARBALL descriptors ({})", latest.size());
                latest.forEach(desc ->
                        log.info("TARBALL descriptor [{}] ({}) -> [{}]", desc.getId(), desc.getLabel(), desc.getBundleName())
                );
            }
            descriptors = latest;
        } catch (Exception e) {
            log.error("Failed to scan for available descriptors:", e);
        }
    }

    public DescriptorServiceImpl(Path archiveFolder, boolean dependencyMatching) {
        this(archiveFolder, null, dependencyMatching);
    }

    public DescriptorServiceImpl(
            Path archiveFolder,
            String defaultDescriptorFile,
            boolean dependencyMatching
    ) {
        this.archiveFolder = archiveFolder;
        this.dependencyMatching = dependencyMatching;
        this.defaultDescriptorFile = defaultDescriptorFile;
        if (defaultDescriptorFile == null) {
            log.warn("Default descriptor is not configured, tarball execution is only possible if descriptor is explicitly specified");
        }
    }

    public List<Descriptor> listDescriptors() {
        try (Stream<Path> files = Files.list(archiveFolder)) {
            return files.filter(path ->
                            path.getFileName().toString().startsWith(DESCRIPTOR_PREFIX)
                    ).filter(Files::isRegularFile)
                    .map(this::deserializeDescriptor)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.info("Error traversing [{}]: {}", archiveFolder, e.getMessage());
            throw new RuntimeException("Error traversing [" + archiveFolder + "]: " + e.getMessage());
        }
    }

    @Override
    public List<Descriptor> getDescriptors(String id) {
        return getDescriptors().stream()
                .filter(descriptor -> Objects.equals(descriptor.getId(), id))
                .collect(Collectors.toList());
    }

    public DescriptorBundle getDescriptorBundle(File dir, Long analysisId, String requestedDescriptorId) {
        List<Descriptor> descriptors = getDescriptors();
        return Optional.ofNullable(
                StringUtils.defaultIfEmpty(requestedDescriptorId, null)
        ).flatMap(id ->
                findRequestedDescriptor(analysisId, descriptors, id)
        ).orElseGet(() -> {
            log.info("For analysis [{}] fall back to dependency matching among {} present descriptors (requested descriptor [{}])", analysisId, descriptors.size(), requestedDescriptorId);
            return findMatchingDescriptor(dir, analysisId, descriptors).orElseGet(() -> {
                log.info("For analysis [{}] fall back to use default descriptor", analysisId);
                return defaultDescriptor().map(this::toBundle).orElseThrow(() -> {
                    log.error("Analysis [{}] aborted. Default descriptor not configured (set property runtimeservice.dist.defaultDescriptorFile)", analysisId);
                    return new RuntimeException("Default descriptor not configured or not found, runtimeservice.dist.defaultDescriptorFile=" + defaultDescriptorFile + ")");
                });
            });
        });
    }

    private Optional<Descriptor> defaultDescriptor() {
        return Optional.ofNullable(defaultDescriptorFile).map(archiveFolder::resolve).filter(Files::isRegularFile).map(this::deserializeDescriptor);
    }

    private Optional<DescriptorBundle> findRequestedDescriptor(Long analysisId, List<Descriptor> available, String id) {
        return available.stream().filter(descriptor ->
                descriptor.getId().equals(id)
        ).reduce((a, b) -> {
            log.error("For analysis [{}], multiple descriptors found for requested id [{}]: [{}] and [{}]",
                    analysisId, id, a.getBundleName(), b.getBundleName());
            throw new RuntimeException("For analysis [" + analysisId + "], multiple descriptors found for requested id [" + id + "]");
        }).map(descriptor -> {
            log.info("For analysis [{}], using requested descriptor [{}] found under [{}]", analysisId, id, descriptor.getBundleName());
            return toBundle(descriptor);
        });
    }

    private Optional<DescriptorBundle> findMatchingDescriptor(File dir, Long analysisId, List<Descriptor> available) {
        return getRuntime(dir).flatMap(runtime -> {
            Map<Boolean, List<Map.Entry<Descriptor, String>>> results = available.stream().flatMap(descriptor ->
                    Optional.ofNullable(descriptor.getExecutionRuntimes()).orElseGet(Collections::emptyList).stream().<Map.Entry<Descriptor, String>>map(runtime1 ->
                            new AbstractMap.SimpleEntry<>(descriptor, runtime1.getMismatches(runtime))
                    )
            ).collect(Collectors.partitioningBy(entry -> entry.getValue() == null));
            List<Map.Entry<Descriptor, String>> matched = results.get(true);
            if (matched.isEmpty()) {
                log.warn("For analysis [{}] of total [{}] descriptors none matched to requested. Fall back to default", analysisId, available.size());
                if (dependencyMatching) {
                    List<Map.Entry<Descriptor, String>> notMatched = results.get(false);
                    notMatched.forEach(mismatch -> {
                        log.info("Descriptor [{}] not matched: {}", mismatch.getKey().getLabel(), mismatch.getValue());
                    });
                }
                return Optional.empty();
            } else {
                return matched.stream().reduce((a, b) -> {
                    log.info("For analysis [{}] multiple descriptors matched. Discarded extra [{}]", analysisId, b.getKey().getBundleName());
                    return a;
                }).map(match -> {
                    log.info("For analysis [{}] using matched descriptor [{}]", analysisId, match.getKey().getBundleName());
                    return match;
                });
            }
        }).map(Map.Entry::getKey).map(this::toBundle);
    }

    private DescriptorBundle toBundle(Descriptor descriptor) {
        String bundleName = descriptor.getBundleName();
        Path path = archiveFolder.resolve(bundleName);
        File bundle = path.toFile();
        if (bundle.exists() && bundle.isFile()) {
            return new DescriptorBundle(path.toString(), descriptor);
        } else {
            log.info("For descriptor [{}] bundle [{}] not found", descriptor.getLabel(), bundle.getName());
            return null;
        }
    }

    private Optional<RExecutionRuntime> getRuntime(File dir) {
        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            return paths.map(Path::toFile).filter(file -> !file.isDirectory()).flatMap(file -> {
                String name = file.getName();
                try (FileInputStream is = new FileInputStream(file)) {
                    return name.endsWith(".zip") ? extractRuntimes(name, is) : getRuntime(file.getPath(), is);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading file [" + name + "]", e);
                }
            }).reduce((a, b) -> {
                log.error("Multiple descriptors found: [{}] and [{}], aborting", a, b);
                throw new RuntimeException("Multiple descriptors found: [" + a + "] and [" + b + "]");
            });
        } catch (IOException e) {
            log.error("Error traversing directory [{}]:", dir.getPath(), e);
            throw new RuntimeException("Error traversing directory [" + dir.getPath() + "]", e);
        }
    }

    private Stream<RExecutionRuntime> extractRuntimes(String zipName, FileInputStream fis) throws IOException {
        Stream.Builder<RExecutionRuntime> sb = Stream.builder();
        try (ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ZipInputSubStream ziss = new ZipInputSubStream(zis);
                getRuntime(zipName + ":/" + entry.getName(), ziss).forEach(sb);
            }
        }
        return sb.build();
    }

    public static Stream<RExecutionRuntime> getRuntime(String name, InputStream is) {
        return Streams.from(
                Optional.ofNullable(PARSE_STRATEGY.apply(name, is)).flatMap(Function.identity())
        ).peek(runtime ->
                log.info("Detected runtime descriptor [{}] in [{}]", runtime, name)
        );
    }

    private Descriptor deserializeDescriptor(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return MAPPER.readValue(is, Descriptor.class);
        } catch (IOException e) {
            throw new RuntimeException("Error getting descriptor from file: " + path, e);
        }
    }

}
