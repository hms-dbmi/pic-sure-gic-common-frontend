package edu.harvard.hms.avillach.passthru.remote;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RemoteResourceService {

    private static final Logger log = LoggerFactory.getLogger(RemoteResourceService.class);

    @PostConstruct
    public void logServices() {
        String sitesStr = "\n\t" + sites.stream().map(RemoteResource::name).collect(Collectors.joining("\n\t"));
        log.info("Passthru resource configured with {} sites: {}", sites.size(), sitesStr);
    }

    private final List<RemoteResource> sites;

    @Autowired
    public RemoteResourceService(List<RemoteResource> sites) {
        this.sites = sites;
    }

    public Optional<RemoteResource> getRemoteResource(UUID commonResource) {
        return sites.stream()
            .filter(r -> r.common().equals(commonResource))
            .findFirst();
    }
}
