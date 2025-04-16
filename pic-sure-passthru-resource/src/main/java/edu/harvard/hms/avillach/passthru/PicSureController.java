package edu.harvard.hms.avillach.passthru;

import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.hms.avillach.passthru.http.HttpRequestService;
import edu.harvard.hms.avillach.passthru.remote.RemoteResource;
import edu.harvard.hms.avillach.passthru.remote.RemoteResourceService;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.Optional;

@Controller
public class PicSureController {
    private static final String BEARER = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(PicSureController.class);

    private final HttpRequestService http;
    private final RemoteResourceService remoteResourceService;

    @Autowired
    public PicSureController(HttpRequestService http, RemoteResourceService remoteResourceService) {
        this.http = http;
        this.remoteResourceService = remoteResourceService;
    }

    @PostMapping("/info")
    public ResponseEntity<ResourceInfo> info(@RequestBody QueryRequest request) {
        return formatRequestAndRunPost(request, "./picsure/info", ResourceInfo.class);
    }

    @PostMapping("/query")
    public ResponseEntity<QueryStatus> query(@RequestBody QueryRequest request) {
        return formatRequestAndRunPost(request, "./picsure/query", QueryStatus.class);
    }

    @PostMapping("/query/{resourceQueryId}/result")
    public ResponseEntity<Object> queryResult(@PathVariable("resourceQueryId") String queryId, QueryRequest request) {
        return formatRequestAndRunPost(request, "./picsure/query/" + queryId + "/result", Object.class);
    }

    @PostMapping("/query/{resourceQueryId}/status")
    public ResponseEntity<QueryStatus> queryStatus(@PathVariable("resourceQueryId") String queryId, QueryRequest request) {
        return formatRequestAndRunPost(request, "./picsure/query/" + queryId + "/status", QueryStatus.class);
    }

    @PostMapping("/query/sync")
    public ResponseEntity<Object> querySync(@RequestBody QueryRequest request) {
        return formatRequestAndRunPost(request, "./picsure/query/sync", Object.class);
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResults> search(@RequestBody QueryRequest request) {
        String path = request == null ? "" : request.getResourceUUID().toString();
        return formatRequestAndRunPost(request, "./picsure/search/" + path, SearchResults.class);
    }

    @PostMapping
    public ResponseEntity<Object> queryFormat(@RequestBody QueryRequest request) {
        String relativePath = "./picsure/query/format";
        return formatRequestAndRunPost(request, relativePath, Object.class);
    }

    private <T> ResponseEntity<T> formatRequestAndRunPost(@RequestBody QueryRequest request, String relativePath, Class<T> returnType) {
        if (request == null || request.getQuery() == null) {
            log.info("Bad request. QueryRequest was null or malformed.");
            return ResponseEntity.badRequest().build();
        }
        Optional<RemoteResource> maybeResource = remoteResourceService.getRemoteResource(request.getResourceUUID());
        if (maybeResource.isEmpty()) {
            log.info("Could not find remote resource with uuid of {}", request.getResourceUUID());
            return ResponseEntity.notFound().build();
        }

        RemoteResource remoteResource = maybeResource.get();
        URI path = remoteResource.base();
        QueryRequest chainRequest = request.copy();
        chainRequest.setResourceUUID(remoteResource.remote());
        return http.post(path, relativePath, chainRequest, returnType, HttpHeaders.AUTHORIZATION, BEARER + remoteResource.token())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.internalServerError().build());
    }
}
