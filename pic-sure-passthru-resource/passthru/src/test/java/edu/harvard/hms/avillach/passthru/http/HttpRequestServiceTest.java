package edu.harvard.hms.avillach.passthru.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Optional;

@SpringBootTest
class HttpRequestServiceTest {

    @Autowired
    HttpRequestService subject;

    @MockitoBean
    HttpClient client;

    @MockitoBean
    HttpClientContext context;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldStopWeirdHeaders() {
        Optional<Object> actual = subject.post(null, new Object(), Object.class, "I'm a headless header :(");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void shouldHandleErrorCodes() throws IOException {
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(500);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(null, new TestObj("Frank"), TestObj.class, "a", "1");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
    }

    @Test
    void shouldPost() throws IOException {
        HttpEntity entity = makeEntity(new TestObj("Bill"));
        HttpResponse response = Mockito.mock(HttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(null, new TestObj("Frank"), TestObj.class, "a", "1");
        Optional<TestObj> expected = Optional.of(new TestObj("Bill"));

        Assertions.assertEquals(expected, actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
    }

    private HttpEntity makeEntity(TestObj obj) throws JsonProcessingException {
        BasicHttpEntity e = new BasicHttpEntity();
        String content = mapper.writeValueAsString(obj);
        e.setContentLength(content.getBytes().length);
        e.setContent(new ByteArrayInputStream(content.getBytes()));
        return e;
    }

    private record TestObj(String name) {}
}