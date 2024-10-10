package com.odysseusinc.arachne.executionengine.execution;

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class ContainerSupport {
    public static final String HOST_DOCKER_INTERNAL = "host.docker.internal";

    /**
     * Detect if we are running in docker and the url is pointing to docker host machine and substitute it.
     * This ensures consistent behaviour in docker containers, as not all of the subsequent steps can process the original url correctly.
     */
    static void patchUrl(DataSourceUnsecuredDTO dataSource) throws ExecutionInitException {
        String jdbcUrl = dataSource.getConnectionString();
        if (jdbcUrl.contains(HOST_DOCKER_INTERNAL)) {
            try {
                String address = InetAddress.getByName(HOST_DOCKER_INTERNAL).getHostAddress();
                String newUrl = jdbcUrl.replace(HOST_DOCKER_INTERNAL, address);
                log.info("Resolved {} = [{}]", HOST_DOCKER_INTERNAL, address);
                dataSource.setConnectionString(newUrl);
            } catch (UnknownHostException e) {
                throw new ExecutionInitException("Unable to resolve to [" + HOST_DOCKER_INTERNAL + "]", e);
            }
        }
    }
}
