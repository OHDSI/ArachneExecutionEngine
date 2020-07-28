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

import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisResultStatusDTO;
import com.odysseusinc.arachne.executionengine.util.FileResourceUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ResultStatusEvaluatorTest {

    private ResultStatusEvaluator resultStatusEvaluator;

    @Before
    public void setUp() {

        resultStatusEvaluator = new ResultStatusEvaluator();
    }

    @Test(expected = NullPointerException.class)
    public void shouldFailOnBadArgument() {

        resultStatusEvaluator.evaluateResultStatus(null);
    }

    @Test
    public void shouldPassAnalysesWithZeroExitCodeAndCleanStdout() {

        final AnalysisResultStatusDTO analysisResult = resultStatusEvaluator.evaluateResultStatus(new RuntimeFinishState(0, "OK"));
        assertThat(analysisResult).isEqualTo(AnalysisResultStatusDTO.EXECUTED);
    }

    @Test
    public void shouldFailOnEmptyStdout() {

        final AnalysisResultStatusDTO analysisResult = resultStatusEvaluator.evaluateResultStatus(new RuntimeFinishState(0, StringUtils.EMPTY));
        assertThat(analysisResult).isEqualTo(AnalysisResultStatusDTO.FAILED);
    }

    @Test
    public void shouldFailOnErrorReportFile() {

        final String output = FileResourceUtils.loadStringResource("/com/odysseusinc/arachne/executionengine/service/failed_stdout_one.txt");
        final AnalysisResultStatusDTO analysisResult = resultStatusEvaluator.evaluateResultStatus(new RuntimeFinishState(0, output));
        assertThat(analysisResult).isEqualTo(AnalysisResultStatusDTO.FAILED);
    }

    @Test
    public void shouldFailOnErrorReportFileCreated() {

        final String output = FileResourceUtils.loadStringResource("/com/odysseusinc/arachne/executionengine/service/failed_stdout_two.txt");
        final AnalysisResultStatusDTO analysisResult = resultStatusEvaluator.evaluateResultStatus(new RuntimeFinishState(0, output));
        assertThat(analysisResult).isEqualTo(AnalysisResultStatusDTO.FAILED);
    }
}