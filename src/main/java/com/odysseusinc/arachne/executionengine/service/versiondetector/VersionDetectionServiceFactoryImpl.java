/*
 *
 * Copyright 2019 Odysseus Data Services, inc.
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
 * Authors: Pavel Grafkin, Vitaly Koulakov, Anastasiia Klochkova, Yaroslav Molodkov, Alexander Cumarav
 * Created: October 21, 2019
 *
 */

package com.odysseusinc.arachne.executionengine.service.versiondetector;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.executionengine.service.VersionDetectionServiceFactory;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VersionDetectionServiceFactoryImpl implements VersionDetectionServiceFactory {

    private final VersionDetectionService versionDetectionService;
    private Map<DBMSType, VersionDetectionService> SERVICE_MAP = new HashMap<>();

    public VersionDetectionServiceFactoryImpl(DefaultVersionDetectionService versionDetectionService,
                                              @Qualifier("impalaVersionDetectionService") VersionDetectionService impalaVersionDetectionService) {

        this.versionDetectionService = versionDetectionService;
        SERVICE_MAP.put(DBMSType.IMPALA, impalaVersionDetectionService);
    }

    @Override
    public VersionDetectionService getService(DBMSType dbmsType) {

        return SERVICE_MAP.getOrDefault(dbmsType, versionDetectionService);
    }
}
