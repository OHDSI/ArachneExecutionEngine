/*
 *
 * Copyright 2020 Odysseus Data Services, inc.
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
 * Authors: Alex Cumarav, Vitaly Koulakov, Yaroslav Molodkov
 * Created: July 27, 2020
 *
 */

package com.odysseusinc.arachne.executionengine.service.impl;

public class RuntimeFinishState {

    private final int exitCode;
    private final String stdout;

    public RuntimeFinishState(int exitCode, String stdout) {

        this.exitCode = exitCode;
        this.stdout = stdout;
    }

    public int getExitCode() {

        return exitCode;
    }

    public String getStdout() {

        return stdout;
    }
}
