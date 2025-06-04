package edu.harvard.hms.avillach.passthru.resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FrontendResourceControllerTest {

    @MockitoBean
    FrontendResourceService frontendResourceService;

    @Autowired
    FrontendResourceController subject;

    @Test
    void getSites() {
        List<CommonAreaResource> resources = List.of(
            new CommonAreaResource("bch", "resource", false)
        );
        Mockito.when(frontendResourceService.getSites())
            .thenReturn(resources);

        ResponseEntity<List<CommonAreaResource>> actual = subject.getSites();

        Assertions.assertEquals(HttpStatusCode.valueOf(200), actual.getStatusCode());
        Assertions.assertEquals(resources, actual.getBody());

    }
}