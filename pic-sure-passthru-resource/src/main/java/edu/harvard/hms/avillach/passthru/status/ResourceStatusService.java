package edu.harvard.hms.avillach.passthru.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class ResourceStatusService {
    private static final Logger log = LoggerFactory.getLogger(ResourceStatusService.class);
    private final ResourceStatusRepository repository;
    private final int backoffBaseTimeSeconds;
    private final int backoffMaxTimeSeconds;

    public ResourceStatusService(
        ResourceStatusRepository repository,
        @Value("${passthru.base_timeout_secs}") int backoffBaseTimeSeconds,
        @Value("${passthru.max_timeout_secs}") int backoffMaxTimeSeconds
    ) {
        this.repository = repository;
        this.backoffBaseTimeSeconds = backoffBaseTimeSeconds;
        this.backoffMaxTimeSeconds = backoffMaxTimeSeconds;
    }

    public boolean isSiteDown(URI site) {
        return switch (repository.getStatus(site).status()) {
            case Status.Online, Status.Unstable -> false;
            case Status.Offline -> true;
        };
    }

    public void markAsOffline(URI site) {
        ResourceStatus oldStatus = repository.getStatus(site);
        log.info("Setting site {} to offline. After {} seconds, it will be set to unstable.", site, oldStatus.backoffSeconds());
        repository.setStatus(site, Status.Offline);
        if (oldStatus.status() == Status.Online) {
            // site was marked as working, so don't double backoff
            Thread.ofVirtual().start(() -> waitThenMarkAsUnstable(site, oldStatus.backoffSeconds()));
        } else if (oldStatus.status() == Status.Unstable) {
            // site was marked as unstable, so double backoff
            int newBackoff = Math.min(oldStatus.backoffSeconds() * 2, backoffMaxTimeSeconds);
            Thread.ofVirtual().start(() -> waitThenMarkAsUnstable(site, newBackoff));
        }
    }

    public void markAsOnline(URI site) {
        if (repository.getStatus(site).status() != Status.Online) {
            log.info("Reporting site {} as back online", site);
            repository.setStatusAndBackoff(site, Status.Online, backoffBaseTimeSeconds);
        }
    }

    private void waitThenMarkAsUnstable(URI site, int backoffSeconds) {
        try {
            Thread.sleep(1000L * backoffSeconds);
        } catch (InterruptedException e) {
            log.error("Error waiting, bailing out and marking site {} as unstable", site);
        }
        repository.setStatusAndBackoff(site, Status.Unstable, backoffSeconds);
    }
}
