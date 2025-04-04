package edu.harvard.hms.avillach.passthru.remote;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "passthru.remote-resource")
@Configuration
public class RemoteResourceConfig {
    private List<RemoteResource> sites;

    @Bean
    public List<RemoteResource> getSites() {
        return sites;
    }

    public void setSites(List<RemoteResource> sites) {
        this.sites = sites;
    }
}
