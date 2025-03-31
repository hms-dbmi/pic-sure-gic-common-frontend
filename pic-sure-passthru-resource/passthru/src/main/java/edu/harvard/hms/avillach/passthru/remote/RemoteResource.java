package edu.harvard.hms.avillach.passthru.remote;

import java.net.URI;
import java.util.UUID;

public record RemoteResource(String name, UUID remote, UUID common, URI base, String token) {
}
