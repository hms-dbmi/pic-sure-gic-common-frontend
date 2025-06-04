package edu.harvard.hms.avillach.passthru.resource;

import edu.harvard.hms.avillach.passthru.status.ResourceStatusService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FrontendResourceServiceTest {

    @MockitoBean
    ResourceStatusService resourceStatusService;

    @Autowired
    FrontendResourceService subject;

    @Test
    void shouldGetSites() {
        URI site = URI.create("https://test_site.invalid");
        UUID uuid = UUID.fromString("87220121-aa95-4fd9-97bf-4a09bd739596");
        Mockito.when(resourceStatusService.isSiteDown(site))
            .thenReturn(false);

        List<CommonAreaResource> actual = subject.getSites();
        List<CommonAreaResource> expected = List.of(new CommonAreaResource("test_site", uuid.toString(), true));

        Assertions.assertEquals(expected, actual);
    }
}