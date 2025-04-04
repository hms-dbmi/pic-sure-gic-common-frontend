package edu.harvard.hms.avillach.passthru.status;

import edu.harvard.hms.avillach.passthru.remote.RemoteResourceService;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Set;

@Service
public class StatusTranslatorService {

    private static final Set<Integer> offlineStatuses = Set.of(504, 503, 408);
    private static final Logger log = LoggerFactory.getLogger(StatusTranslatorService.class);
    private final ResourceStatusService statusService;

    @Autowired
    public StatusTranslatorService(ResourceStatusService statusService) {
        this.statusService = statusService;
    }

    public void translateResponseAndSetStatus(Exception e, HttpResponse response, URI site) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            log.warn("Got exception {} which indicates server offline. Marking site accordingly", e.getClass().getSimpleName());
            statusService.markAsOffline(site);
        } else if (
            response != null && response.getStatusLine() != null &&
            offlineStatuses.contains(response.getStatusLine().getStatusCode())
        ) {
            log.warn("Got a status code {} which indicated service offline. Marking side accordingly", response.getStatusLine().getStatusCode());
            statusService.markAsOffline(site);
        } else {
            statusService.markAsOnline(site);
        }
    }
}
