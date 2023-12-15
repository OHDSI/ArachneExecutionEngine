/*
 *
 * Copyright 2018 Odysseus Data Services, inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Company: Odysseus Data Services, Inc.
 * Product Owner/Architecture: Gregory Klebanov
 * Authors: Maria Pozhidaeva
 * Created: May 16, 2018
 *
 */

package com.odysseusinc.arachne.executionengine.aspect;

import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FileDescriptorCountAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDescriptorCountAspect.class);

    @Value("${logging.descriptor.count.enabled}")
    private boolean enabled;

    @Around("@annotation(com.odysseusinc.arachne.executionengine.aspect.FileDescriptorCount)")
    public Object log(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        if (enabled) {
            log("Before " + proceedingJoinPoint.getSignature().toString());
        }
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            if (enabled) {
                log("After " + proceedingJoinPoint.getSignature().toString());
            }
        }
    }

    private void log(String message) {

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            if (operatingSystemMXBean instanceof UnixOperatingSystemMXBean) {
                UnixOperatingSystemMXBean osMxBean = (UnixOperatingSystemMXBean) operatingSystemMXBean;
                LOGGER.info("{}: open file descriptor count [{}] from max [{}]", message, osMxBean.getOpenFileDescriptorCount(),
                        osMxBean.getMaxFileDescriptorCount());
            } else {
                LOGGER.info("Descriptor count is not supported");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to log descriptor count: ", e);
        }
    }
}
