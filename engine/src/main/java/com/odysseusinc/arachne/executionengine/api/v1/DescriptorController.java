package com.odysseusinc.arachne.executionengine.api.v1;

import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorDTO;
import com.odysseusinc.arachne.execution_engine_common.descriptor.dto.RuntimeEnvironmentDescriptorsDTO;
import com.odysseusinc.arachne.executionengine.model.descriptor.Descriptor;
import com.odysseusinc.arachne.executionengine.model.descriptor.converter.DescriptorConverter;
import com.odysseusinc.arachne.executionengine.service.DescriptorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api
@RequestMapping(value = DescriptorController.REST_API_MAIN)
public class DescriptorController {

    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_MAIN = "/api/v1";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_DESCRIPTORS = "/descriptors";
    @SuppressWarnings("WeakerAccess")
    public static final String REST_API_DESCRIPTOR = "/descriptors/{id}";

    private final DescriptorService descriptorService;

    private static final DescriptorConverter descriptorConverter = new DescriptorConverter();

    @Value("${docker.enable:false}")
    private boolean useDocker;

    public DescriptorController(DescriptorService descriptorService) {
        this.descriptorService = descriptorService;
    }

    @ApiOperation(value = "Runtimes for analysis")
    @RequestMapping(value = REST_API_DESCRIPTORS, method = RequestMethod.GET)
    public RuntimeEnvironmentDescriptorsDTO getDescriptors() {
        Stream<Descriptor> descriptors = descriptorService.getDescriptors().map(Collection::stream).orElseGet(Stream::of);
        return new RuntimeEnvironmentDescriptorsDTO(useDocker, descriptors.map(descriptorConverter::toDto).collect(Collectors.toList()));
    }

    @ApiOperation(value = "Runtimes with identifier for analysis")
    @RequestMapping(value = REST_API_DESCRIPTOR, method = RequestMethod.GET)
    public RuntimeEnvironmentDescriptorsDTO getDescriptors(@PathVariable String id) {
        List<Descriptor> descriptors = descriptorService.getDescriptors(id);
        List<RuntimeEnvironmentDescriptorDTO> descriptorDTOS = descriptors.stream()
                .map(descriptorConverter::toDto)
                .collect(Collectors.toList());
        return new RuntimeEnvironmentDescriptorsDTO(useDocker, descriptorDTOS);
    }
}
