package edu.harvard.hms.avillach.passthru.resource;

import edu.harvard.hms.avillach.passthru.status.ResourceStatus;

public record CommonAreaResource(String name, String resourceUUID, boolean online) {
}
