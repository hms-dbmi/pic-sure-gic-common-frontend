package edu.harvard.hms.avillach.passthru;

import edu.harvard.dbmi.avillach.domain.GeneralQueryRequest;
import edu.harvard.dbmi.avillach.domain.QueryRequest;
import edu.harvard.dbmi.avillach.domain.QueryStatus;
import edu.harvard.dbmi.avillach.domain.SearchResults;
import edu.harvard.hms.avillach.passthru.http.HttpRequestService;
import edu.harvard.hms.avillach.passthru.remote.RemoteResource;
import edu.harvard.hms.avillach.passthru.remote.RemoteResourceService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class PicSureControllerTest {

    UUID common = UUID.fromString("f29529a0-d8c7-4fed-8b27-0beccd706dad");
    UUID remote = UUID.fromString("87220121-aa95-4fd9-97bf-4a09bd739596");

    @MockitoBean
    HttpRequestService requestService;

    @MockitoBean
    RemoteResourceService remoteResourceService;

    @Autowired
    PicSureController subject;

    @Test
    void shouldBailOnNullQuery() {
        ResponseEntity<Object> actual = subject.queryFormat(null);
        ResponseEntity<Object> expected = ResponseEntity.badRequest().build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldBailOnEmptyQuery() {
        QueryRequest request = new GeneralQueryRequest();
        request.setQuery(null);

        ResponseEntity<StreamingResponseBody> actual = subject.querySync(request);
        ResponseEntity<Object> expected = ResponseEntity.badRequest().build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldBailIfSiteDNE() {
        UUID siteUUID = UUID.fromString("f29529a0-d8c7-4fed-8b27-0beccd706dad");
        QueryRequest request = new GeneralQueryRequest();
        request.setQuery(new Object());
        request.setResourceUUID(siteUUID);
        Mockito.when(remoteResourceService.getRemoteResource(siteUUID))
            .thenReturn(Optional.empty());

        ResponseEntity<SearchResults> actual = subject.search(request);
        ResponseEntity<SearchResults> expected = ResponseEntity.notFound().build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void should400IfSiteFails() {
        URI site = URI.create("bch.invalid");
        RemoteResource resource = new RemoteResource("bch", common, remote, site, "token");
        QueryRequest request = new GeneralQueryRequest();
        request.setQuery(new Object());
        request.setResourceUUID(common);
        Mockito.when(remoteResourceService.getRemoteResource(common))
            .thenReturn(Optional.of(resource));

        QueryRequest chain = request.copy();
        chain.setResourceUUID(remote);
        Mockito.when(requestService.post(site, "./query/query-id/status", chain, QueryStatus.class, HttpHeaders.AUTHORIZATION, "Bearer token"))
            .thenReturn(Optional.empty());

        ResponseEntity<QueryStatus> actual = subject.queryStatus("query-id", request);
        ResponseEntity<QueryStatus> expected = ResponseEntity.internalServerError().build();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldWorkIfSiteWorks() {
        URI site = URI.create("bch.invalid");
        RemoteResource resource = new RemoteResource("bch", common, remote, site, "token");
        QueryRequest request = new GeneralQueryRequest();
        request.setQuery(new Object());
        request.setResourceUUID(common);
        Mockito.when(remoteResourceService.getRemoteResource(common))
            .thenReturn(Optional.of(resource));

        QueryRequest chain = request.copy();
        chain.setResourceUUID(remote);
        Mockito.when(requestService.post(
                eq(site), eq("./query/query-id/result"), Mockito.any(QueryRequest.class),
                eq(Object.class), eq(HttpHeaders.AUTHORIZATION), eq("Bearer token")
            ))
            .thenReturn(Optional.of(":)"));

        ResponseEntity<Object> actual = subject.queryResult("query-id", request);
        ResponseEntity<Object> expected = ResponseEntity.ok().body(":)");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void shouldDoDictionaryPost() {
        URI site = URI.create("bch.invalid");
        RemoteResource resource = new RemoteResource("bch", common, remote, site, "token");
        Mockito.when(remoteResourceService.getRemoteResource("bch"))
            .thenReturn(Optional.of(resource));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("/dictionary-dump/bch/my/path/with/slashes"));
        Mockito.when(request.getQueryString()).thenReturn("?foo=asda%20foo&bar=asda");
        Mockito.when(requestService.post(
                eq(site), eq("./proxy/dictionary-dump/my/path/with/slashes?foo=asda foo&bar=asda"),
                eq(":)"), eq(String.class), eq(HttpHeaders.AUTHORIZATION), eq("Bearer token")
            ))
            .thenReturn(Optional.of(":)"));

        ResponseEntity<String> response = subject.postDictionaryRequest(":)", "bch", request);

        Assertions.assertEquals(200, response.getStatusCode().value());
        Assertions.assertEquals(":)", response.getBody());
    }

    @Test
    void shouldNotDoInvalidDictionaryPost() {
        Mockito.when(remoteResourceService.getRemoteResource("bch"))
            .thenReturn(Optional.empty());

        ResponseEntity<String> response =
            subject.postDictionaryRequest(":)", "bch", Mockito.mock(HttpServletRequest.class));

        Assertions.assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void shouldDoDictionaryGet() throws IOException {
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        Mockito.when(entity.getContent()).thenReturn(new ByteArrayInputStream(":)".getBytes()));
        CloseableHttpResponse rawResponse = Mockito.mock(CloseableHttpResponse.class);
        Mockito.when(rawResponse.getEntity())
            .thenReturn(entity);
        Mockito.when(rawResponse.getStatusLine())
            .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "peachy"));

        URI site = URI.create("bch.invalid");
        RemoteResource resource = new RemoteResource("bch", common, remote, site, "token");
        Mockito.when(remoteResourceService.getRemoteResource("bch"))
            .thenReturn(Optional.of(resource));
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("/dictionary-dump/bch/my/path/with/slashes"));
        Mockito.when(request.getQueryString()).thenReturn("?foo=asda%20foo&bar=asda");

        Mockito.when(requestService.getRaw(
                eq(site), eq("./proxy/dictionary-dump/my/path/with/slashes?foo=asda foo&bar=asda"),
                eq(HttpHeaders.AUTHORIZATION), eq("Bearer token")
            ))
            .thenReturn(Optional.of(rawResponse));


        ResponseEntity<StreamingResponseBody> response = subject.getDictionaryRequest("bch", request);

        Assertions.assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void shouldNotDoInvalidDictionaryGet() {
        Mockito.when(remoteResourceService.getRemoteResource("bch"))
            .thenReturn(Optional.empty());


        ResponseEntity<StreamingResponseBody> response =
            subject.getDictionaryRequest("bch", Mockito.mock(HttpServletRequest.class));

        Assertions.assertEquals(404, response.getStatusCode().value());
    }
}
