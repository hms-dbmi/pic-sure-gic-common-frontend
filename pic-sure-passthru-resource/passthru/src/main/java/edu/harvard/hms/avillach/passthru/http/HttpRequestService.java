package edu.harvard.hms.avillach.passthru.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.hms.avillach.passthru.status.ResourceStatusService;
import edu.harvard.hms.avillach.passthru.status.StatusTranslatorService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Gatherers;

@Service
public class HttpRequestService {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestService.class);
    private final HttpClient client;
    private final HttpClientContext context;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ResourceStatusService statusService;
    private final StatusTranslatorService translatorService;

    @Autowired
    public HttpRequestService(HttpClient client, HttpClientContext context, ResourceStatusService statusService, StatusTranslatorService translatorService) {
        this.client = client;
        this.context = context;
        this.statusService = statusService;
        this.translatorService = translatorService;
    }

    public <T> Optional<T> post(URI site, String path, Object body, @NonNull Class<T> responseType, String... headers) {
        if (statusService.isSiteDown(site)) {
            log.info("Site marked as down. Short circuiting to failed request.");
            return Optional.empty();
        }
        if (headers.length % 2 == 1) {
            log.error("Headers should be sent in key value pairs. Got this: {}", String.join(", ", headers));
            return Optional.empty();
        }
        HttpPost request = new HttpPost(site.resolve(path));
        Arrays.stream(headers)
            .gather(Gatherers.windowFixed(2))
            .map(pair -> new BasicHeader(pair.get(0), pair.get(1)))
            .forEach(request::setHeader);

        Exception ex = null;
        HttpResponse response = null;
        try {
            String bodyStr = mapper.writeValueAsString(body);
            request.setEntity(new StringEntity(bodyStr));
            request.setHeader("Content-Type", "application/json");
            response = client.execute(request, context);
            if (isErrored(response)) {
                return Optional.empty();
            }
            String entityStr = EntityUtils.toString(response.getEntity());
            return Optional.ofNullable(mapper.readValue(entityStr, responseType));
        } catch (ConnectTimeoutException | SocketTimeoutException e) {
            ex = e;
        } catch (IOException e) {
            log.warn("Error writing, encoding or sending request: ", e);
            ex = e;
        } finally {
            translatorService.translateResponseAndSetStatus(ex, response, site);
        }

        return Optional.empty();
    }

    private static boolean isErrored(HttpResponse response) {
        return switch (response.getStatusLine().getStatusCode() / 100) {
            case 1, 3 -> {
                log.info("Strange response code, will attempt to complete req: {}", response.getStatusLine().getStatusCode());
                yield false;
            }
            case 2 -> false;
            default -> {
                log.info("Error code for http request. Will not attempt to complete request. Code: {}", response.getStatusLine().getStatusCode());
                yield true;
            }
        };
    }
}
