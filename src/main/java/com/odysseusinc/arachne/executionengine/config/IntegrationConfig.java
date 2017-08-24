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

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
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