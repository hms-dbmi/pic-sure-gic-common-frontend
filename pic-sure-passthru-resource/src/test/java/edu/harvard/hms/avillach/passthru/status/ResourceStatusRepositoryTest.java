package edu.harvard.hms.avillach.passthru.status;


import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.net.ssl.SSLContext;
import java.net.URI;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceStatusRepositoryTest {

    @Autowired
    private ResourceStatusRepository subject;

    @MockitoBean
    SSLContext context;

    @Value("${passthru.base_timeout_secs}")
    int backoffBaseTimeSeconds;


    @Test
    @Order(1)
    void shouldGetDefaultStatus() {
        ResourceStatus actual = subject.getStatus(URI.create("foo.invalid"));
        ResourceStatus expected = new ResourceStatus(URI.create("foo.invalid"), Status.Online, backoffBaseTimeSeconds);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @Order(2)
    void shouldSetAndGetStatus() {
        URI site = URI.create("foo.invalid");
        subject.setStatus(site, Status.Offline);
        ResourceStatus actual = subject.getStatus(site);
        ResourceStatus expected = new ResourceStatus(site, Status.Offline, backoffBaseTimeSeconds);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @Order(3)
    void shouldSetAndGetStatusAndBackoff() {
        URI site = URI.create("foo.invalid");
        subject.setStatusAndBackoff(site, Status.Offline, 64);
        ResourceStatus actual = subject.getStatus(site);
        ResourceStatus expected = new ResourceStatus(site, Status.Offline, 64);

        Assertions.assertEquals(expected, actual);
    }
}