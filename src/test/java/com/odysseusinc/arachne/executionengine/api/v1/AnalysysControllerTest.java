/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: April 03, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.api.v1;

import com.odysseusinc.arachne.commons.types.DBMSType;
import com.odysseusinc.arachne.executionengine.ExecutionEngineStarter;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestStatusDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.AnalysisRequestTypeDTO;
import com.odysseusinc.arachne.execution_engine_common.api.v1.dto.DataSourceUnsecuredDTO;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@SpringBootTest(classes = ExecutionEngineStarter.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "classpath:application-test.properties")
public class AnalysysControllerTest {
    private final static Logger log = LoggerFactory.getLogger(AnalysysControllerTest.class);
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Value("${server.port}")
    private Integer serverPort;
    private static RestTemplate restTemplate;

    private static final String BASE_URL = "http://localhost";

    static volatile AtomicBoolean updateStatusIsOk;
    static volatile AtomicBoolean resultIsOk;
    static CountDownLatch latch;

    @BeforeClass
    public static void getRestTemplate() {

        RestTemplate template = new RestTemplate();
        restTemplate = template;
        template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    @Before
    public void setResultIsOk() {

        latch = new CountDownLatch(2);
        updateStatusIsOk = new AtomicBoolean(false);
        resultIsOk = new AtomicBoolean(false);
    }

    @Test
    public void test01AnalysisController_Analyze() {

        String URL = BASE_URL + ":" + serverPort + AnalisysController.REST_API_MAIN + AnalisysController.REST_API_ANALYZE;
        AnalysisRequestDTO analysis = getAnalysis();
        analysis.setExecutableFileName("MainAnalysis.R");
        analysis.setId(1L);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(analysis, getRFiles());
        ResponseEntity<AnalysisRequestStatusDTO> exchange = restTemplate.exchange(
                URL,
                HttpMethod.POST, requestEntity,
                AnalysisRequestStatusDTO.class);
        Assert.assertEquals(AnalysisRequestTypeDTO.R, exchange.getBody().getType());
        AssertStates();
    }

    @Test
    public void test02AnalysisController_Analyze() {

        String URL = BASE_URL + ":" + serverPort + AnalisysController.REST_API_MAIN + AnalisysController.REST_API_ANALYZE;
        AnalysisRequestDTO analysis = getAnalysis();
        analysis.setExecutableFileName("SQL_Request.sql");
        analysis.setId(2L);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(analysis, getSqlFiles());
        ResponseEntity<AnalysisRequestStatusDTO> exchange = restTemplate.exchange(
                URL,
                HttpMethod.POST, requestEntity,
                AnalysisRequestStatusDTO.class);
        Assert.assertEquals(AnalysisRequestTypeDTO.SQL, exchange.getBody().getType());
        AssertStates();
    }

    @Test
    public void test03AnalysisController_AnalyzeValidationError() {

        thrown.expect(HttpClientErrorException.class);
        thrown.expectMessage("422 null");
        String URL = BASE_URL + ":" + serverPort + AnalisysController.REST_API_MAIN + AnalisysController.REST_API_ANALYZE;
        AnalysisRequestDTO analysis = getAnalysis();
        analysis.setId(null);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = getRequestEntity(analysis, null);
        ResponseEntity<String> exchange = restTemplate.exchange(
                URL,
                HttpMethod.POST, requestEntity,
                String.class);
    }

    private void AssertStates() {

        try {
            latch.await(120, TimeUnit.SECONDS);
            Assert.assertTrue(updateStatusIsOk.get());
            latch.await(120, TimeUnit.SECONDS);
            Assert.assertTrue(resultIsOk.get());
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

    private AnalysisRequestDTO getAnalysis() {

        AnalysisRequestDTO analysis = new AnalysisRequestDTO();
        analysis.setId(5L);
        analysis.setExecutableFileName("MainAnalysis.R");
        analysis.setCallbackPassword("password");
        analysis.setUpdateStatusCallback(BASE_URL + ":" + serverPort + "/submissions/{id}/update/{password}");
        analysis.setResultCallback(BASE_URL + ":" + serverPort + "/submissions/{id}/result/{password}");
        DataSourceUnsecuredDTO dataSource = new DataSourceUnsecuredDTO();
        dataSource.setType(DBMSType.POSTGRESQL);
        dataSource.setConnectionString("");
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        dataSource.setCdmSchema("V4_0");
        analysis.setDataSource(dataSource);
        return analysis;
    }

    private static HttpEntity<LinkedMultiValueMap<String, Object>> getRequestEntity(AnalysisRequestDTO analysis, ClassPathResource... files) {

        HttpHeaders jsonHeader = new HttpHeaders();
        jsonHeader.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AnalysisRequestDTO> analysisRequestHttpEntity = new HttpEntity<>(analysis, jsonHeader);
        LinkedMultiValueMap<String, Object> multipartRequest = new LinkedMultiValueMap<>();
        multipartRequest.add("analysisRequest", analysisRequestHttpEntity);
        if (files != null) {
            Arrays.stream(files).forEach(f -> multipartRequest.add("file", f));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(multipartRequest, headers);
    }

    private static ClassPathResource[] getRFiles() {

        return new ClassPathResource[]{
                new ClassPathResource("analysisRequest/MainAnalysis.R"),
                new ClassPathResource("analysisRequest/HelperFunctions.R"),
                new ClassPathResource("analysisRequest/TxPath parameterized.sql"),
        };
    }

    private static ClassPathResource[] getSqlFiles() {

        return new ClassPathResource[]{
                new ClassPathResource("sqlRequest/SQL_Request.sql"),
                new ClassPathResource("sqlRequest/SQL_Request_2.sql"),
                new ClassPathResource("sqlRequest/SQL_Request_3.sql")
        };
    }

}
