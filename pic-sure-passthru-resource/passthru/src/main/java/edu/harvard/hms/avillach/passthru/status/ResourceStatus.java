package edu.harvard.hms.avillach.passthru.status;

import java.net.URI;
import java.util.UUID;

public record ResourceStatus(URI uri, Status status, int backoffSeconds) {
    public ResourceStatus withStatus(Status status) {
        return new ResourceStatus(uri, status, backoffSeconds);
    }

    public ResourceStatus withBackoff(int backoffSeconds) {
        return new ResourceStatus(uri, status, backoffSeconds);
    }
}
