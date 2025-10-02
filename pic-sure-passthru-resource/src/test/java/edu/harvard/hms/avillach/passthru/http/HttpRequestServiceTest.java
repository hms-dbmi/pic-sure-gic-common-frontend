package edu.harvard.hms.avillach.passthru.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.hms.avillach.passthru.status.ResourceStatusService;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@SpringBootTest
class HttpRequestServiceTest {

    @Autowired
    HttpRequestService subject;

    @MockitoBean
    CloseableHttpClient client;
    
    @MockitoBean
    ResourceStatusService statusService;

    @MockitoBean
    HttpClientContext context;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldStopWeirdHeaders() {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        Optional<Object> actual = subject.post(URI.create("foo.invalid"), "./", new Object(), Object.class, "I'm a headless header :(");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void shouldHandleErrorCodes() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(500);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(URI.create("foo.invalid"), "./", new TestObj("Frank"), TestObj.class, "a", "1");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
    }

    @Test
    void shouldPost() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        HttpEntity entity = makeEntity(new TestObj("Bill"));
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(URI.create("foo.invalid"), "./", new TestObj("Frank"), TestObj.class, "a", "1");
        Optional<TestObj> expected = Optional.of(new TestObj("Bill"));

        Assertions.assertEquals(expected, actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
    }

    @Test
    void shouldPostForWeirdCodes() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        HttpEntity entity = makeEntity(new TestObj("Bill"));
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(100);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(URI.create("foo.invalid"), "./", new TestObj("Frank"), TestObj.class, "a", "1");
        Optional<TestObj> expected = Optional.of(new TestObj("Bill"));

        Assertions.assertEquals(expected, actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
    }

    @Test
    void shouldNotPostIfSiteIsDown() {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(true);
        Optional<Object> actual = subject.post(URI.create("foo.invalid"), "./", new Object(), Object.class);

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void shouldMarkSiteASDownForCodes() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(504); // simulate site timeout
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<TestObj> actual = subject.post(URI.create("foo.invalid"), "./", new TestObj("Frank"), TestObj.class, "a", "1");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
        Mockito.verify(statusService, Mockito.times(1))
            .markAsOffline(URI.create("foo.invalid"));
    }

    @Test
    void shouldMarkSiteASDownForException() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200); // ok http code, but throw exception
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
            .thenThrow(new ConnectTimeoutException());

        Optional<TestObj> actual = subject.post(URI.create("foo.invalid"), "./", new TestObj("Frank"), TestObj.class, "a", "1");

        Assertions.assertEquals(Optional.empty(), actual);
        Mockito.verify(client, Mockito.times(1))
            .execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class));
        Mockito.verify(statusService, Mockito.times(1))
            .markAsOffline(URI.create("foo.invalid"));
    }

    @Test
    void shouldGetRaw() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        HttpEntity entity = makeEntity(new TestObj("Bill"));
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(client.execute(Mockito.any(HttpGet.class), Mockito.any(HttpClientContext.class)))
            .thenReturn(response);

        Optional<CloseableHttpResponse> actual = subject.getRaw(URI.create("foo.invalid"), "./", "a", "1");

        String jsonActual = new String(actual.get().getEntity().getContent().readAllBytes());
        String jsonExpected = "{\"name\":\"Bill\"}";
        Assertions.assertEquals(jsonExpected, jsonActual);
    }

    @Test
    void shouldGetRawSiteDown() {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(true);

        Optional<CloseableHttpResponse> actual = subject.getRaw(URI.create("foo.invalid"), "./", "a", "1");

        Optional<CloseableHttpResponse> expected = Optional.empty();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldGetRawBadHEaders() {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);

        Optional<CloseableHttpResponse> actual = subject.getRaw(URI.create("foo.invalid"), "./", "a");

        Optional<CloseableHttpResponse> expected = Optional.empty();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldPostRaw() throws IOException {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);
        HttpEntity entity = makeEntity(new TestObj("Bill"));
        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        Mockito.when(status.getStatusCode()).thenReturn(200);
        Mockito.when(response.getStatusLine()).thenReturn(status);
        Mockito.when(response.getEntity()).thenReturn(entity);
        Mockito.when(client.execute(Mockito.any(HttpPost.class), Mockito.any(HttpClientContext.class)))
                .thenReturn(response);

        Optional<CloseableHttpResponse> actual = subject.postRaw(URI.create("foo.invalid"), "./", "{}", "a", "1");

        String jsonActual = new String(actual.get().getEntity().getContent().readAllBytes());
        String jsonExpected = "{\"name\":\"Bill\"}";
        Assertions.assertEquals(jsonExpected, jsonActual);
    }

    @Test
    void shouldPostRawSiteDown() {
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(true);

        Optional<CloseableHttpResponse> actual = subject.postRaw(URI.create("foo.invalid"), "./", "", "a", "1");

        Optional<CloseableHttpResponse> expected = Optional.empty();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldPostRawBadHeaders(){
        Mockito.when(statusService.isSiteDown(URI.create("foo.invalid"))).thenReturn(false);

        Optional<CloseableHttpResponse> actual = subject.postRaw(URI.create("foo.invalid"), "./", "", "a");
        Optional<CloseableHttpResponse> expected = Optional.empty();
        Assertions.assertEquals(expected, actual);
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