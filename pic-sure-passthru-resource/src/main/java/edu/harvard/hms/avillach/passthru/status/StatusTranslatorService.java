package edu.harvard.hms.avillach.passthru.status;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    public void translateResponseAndSetStatus(Exception e, Integer responseCode, URI site) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException || e instanceof HttpHostConnectException) {
            log.warn("Got exception {} which indicates server offline. Marking site accordingly", e.getClass().getSimpleName());
            statusService.markAsOffline(site);
        } else if (responseCode != null && offlineStatuses.contains(responseCode)) {
            log.warn("Got a status code {} which indicated service offline. Marking side accordingly", responseCode);
            statusService.markAsOffline(site);
        } else {
            statusService.markAsOnline(site);
        }
    }
}
