package edu.harvard.hms.avillach.passthru.remote;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest
class RemoteResourceServiceTest {

    @Autowired
    RemoteResourceService subject;

    @Test
    void shouldGetSiteFromConfig() throws MalformedURLException {
        UUID common = UUID.fromString("87220121-aa95-4fd9-97bf-4a09bd739596");
        UUID remote = UUID.fromString("f29529a0-d8c7-4fed-8b27-0beccd706dad");
        URI baseURL = URI.create("https://test_site.invalid");

        Optional<RemoteResource> actual = subject.getRemoteResource(common);
        RemoteResource expected =
            new RemoteResource("test_site", remote, common, baseURL, "im a token :)");

        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(expected, actual.get());
    }

    @Test
    void shouldGetSiteByName() {
        UUID common = UUID.fromString("87220121-aa95-4fd9-97bf-4a09bd739596");
        UUID remote = UUID.fromString("f29529a0-d8c7-4fed-8b27-0beccd706dad");
        URI baseURL = URI.create("https://test_site.invalid");

        Optional<RemoteResource> actual = subject.getRemoteResource("test_site");
        RemoteResource expected =
            new RemoteResource("test_site", remote, common, baseURL, "im a token :)");

        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(expected, actual.get());
    }
}