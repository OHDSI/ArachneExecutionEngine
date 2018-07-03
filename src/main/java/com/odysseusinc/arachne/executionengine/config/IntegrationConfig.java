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
 * Authors: Pavel Grafkin, Alexandr Ryabokon, Vitaly Koulakov, Anton Gackovka, Maria Pozhidaeva, Mikhail Mironov
 * Created: March 24, 2017
 *
 */

package com.odysseusinc.arachne.executionengine.config;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


@Configuration
public class IntegrationConfig {

    @Bean(name = "nodeRestTemplate")
    public RestTemplate centralRestTemplate(HttpClient httpClient) {

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        // https://jira.spring.io/browse/SPR-7909
        // Not to put posted files into memory, which can cause Heap overflow in case of big files
        requestFactory.setBufferRequestBody(false);
        return new RestTemplate(requestFactory);
    }

    @Configuration
    @ConditionalOnProperty(value = "server.ssl.strictMode", havingValue = "false")
    public class nonStrictSSLSecurityConfig {
        @Bean
        public HttpClient getHttpClient() {

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {

                            return null;
                        }

                        public void checkClientTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {

                        }

                        public void checkServerTrusted(
                                java.security.cert.X509Certificate[] certs, String authType) {

                        }
                    }
            };

            SSLConnectionSocketFactory csf = null;
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                csf = new SSLConnectionSocketFactory(sc, (s, sslSession) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
            return HttpClients.custom().setSSLSocketFactory(csf).build();
        }
    }

    @Configuration
    @ConditionalOnProperty(value = "server.ssl.strictMode", havingValue = "true")
    public class strictSSLSecurityConfig {

        @Bean
        public HttpClient getHttpClient() {

            return HttpClients.createDefault();
        }
    }
}