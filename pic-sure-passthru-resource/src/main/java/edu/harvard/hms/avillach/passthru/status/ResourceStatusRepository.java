package edu.harvard.hms.avillach.passthru.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ResourceStatusRepository {
    private final ConcurrentHashMap<URI, ResourceStatus> statuses = new ConcurrentHashMap<>();
    private final int backoffBaseTimeSeconds;

    @Autowired
    public ResourceStatusRepository(@Value("${passthru.base_timeout_secs}") int backoffBaseTimeSeconds) {
        this.backoffBaseTimeSeconds = backoffBaseTimeSeconds;
    }

    public void setStatus(URI key, Status status) {
        statuses.put(key, getStatus(key).withStatus(status));
    }

    public void setStatusAndBackoff(URI key, Status status, int backoffSecs) {
        ResourceStatus updated = getStatus(key)
            .withBackoff(backoffSecs)
            .withStatus(status);
        statuses.put(key, updated);
    }

    public ResourceStatus getStatus(URI key) {
        return statuses.getOrDefault(key, new ResourceStatus(key, Status.Online, backoffBaseTimeSeconds));
    }
}
