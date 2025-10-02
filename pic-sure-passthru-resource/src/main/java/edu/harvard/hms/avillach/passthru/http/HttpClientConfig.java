package edu.harvard.hms.avillach.passthru.http;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

@Configuration
public class HttpClientConfig {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConfig.class);

    @Value("${http.proxyUser:}")
    private String proxyUser;

    @Value("${http.proxyPassword:}")
    private String proxyPassword;

    @Value("${http.keystore.password:password}")
    private String keystorePassword;

    @Bean
    public SSLContext sslContext() throws NoSuchAlgorithmException {
        return SSLContext.getDefault();
    }

    @Bean
    public CloseableHttpClient getHttpClient(@Autowired SSLContext context) {
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        manager.setMaxTotal(100);
        if (!StringUtils.hasLength(proxyUser)) {
            LOG.info("No proxy user found, making default client.");
            return HttpClients.custom().setConnectionManager(manager).build();
        }
        LOG.info("Found proxy user {}, will configure proxy", proxyUser);

        return HttpClients
            .custom()
            .setConnectionManager(new PoolingHttpClientConnectionManager())
            .useSystemProperties()
            .setSSLContext(context)
            .build();
    }

    @Bean
    public HttpClientContext getClientConfig() {
        if (StringUtils.hasLength(proxyUser) && StringUtils.hasLength(proxyPassword)) {
            HttpClientContext httpClientContext = HttpClientContext.create();
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxyUser, proxyPassword));
            httpClientContext.setCredentialsProvider(credentialsProvider);

            return httpClientContext;
        }
        return HttpClientContext.create();
    }
}
