package edu.harvard.hms.avillach.passthru.status;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;

@SpringBootTest
class StatusTranslatorServiceTest {

    @MockitoBean
    ResourceStatusService statusService;

    @Autowired
    StatusTranslatorService subject;

    private final URI site = URI.create("foo.invalid");

    @Test
    void shouldMarkOfflineForTimeoutEx() {
        subject.translateResponseAndSetStatus(new ConnectTimeoutException(), null, site);

        Mockito.verify(statusService, Mockito.times(1)).markAsOffline(site);
    }

    @Test
    void shouldMarkOfflineForCode() {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(504);
        Mockito.when(response.getStatusLine()).thenReturn(status);

        subject.translateResponseAndSetStatus(null, response, site);

        Mockito.verify(statusService, Mockito.times(1)).markAsOffline(site);
    }

    @Test
    void shouldMarkOnline() {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200);
        Mockito.when(response.getStatusLine()).thenReturn(status);

        subject.translateResponseAndSetStatus(null, response, site);

        Mockito.verify(statusService, Mockito.times(1)).markAsOnline(site);
    }
}