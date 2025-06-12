package edu.harvard.hms.avillach.passthru.http;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
    @Qualifier("default")
    public CloseableHttpClient getHttpClient(@Autowired SSLContext context) throws KeyManagementException, NoSuchAlgorithmException {
        // Create trust manager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }
        };

        // Create SSL context with trust-all manager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // Create HttpClient with relaxed SSL settings
        return HttpClients.custom()
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .build();
    }

    @Bean()
    @Qualifier("no-timeout")
    public CloseableHttpClient getNoTimeoutHttpClient(@Autowired SSLContext context) throws KeyManagementException, NoSuchAlgorithmException {
        // Create trust manager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }
        };

        // Create SSL context with trust-all manager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(0)  // No timeout for getting a connection from pool
            .setConnectTimeout(0)            // No timeout for establishing connection
            .setSocketTimeout(0)             // No timeout between packets
            .build();

        // Create HttpClient with relaxed SSL settings
        return HttpClients.custom()
            .setSSLContext(sslContext)
            .setDefaultRequestConfig(config)
            .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
            .build();
    }

    @Bean
    public SSLContext configureSecurityContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                null, // No KeyManager needed
                new TrustManager[] { new TrustAllCertificates() }, // Trust ALL certs
                null  // Default SecureRandom
            );
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.info("Could not create security context: ", e);
        }
        try {
            return SSLContextBuilder.create().build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public class TrustAllCertificates implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
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
