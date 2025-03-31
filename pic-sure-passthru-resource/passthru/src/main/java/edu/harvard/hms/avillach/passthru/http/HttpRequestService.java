package edu.harvard.hms.avillach.passthru.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

    @Autowired
    public HttpRequestService(HttpClient client, HttpClientContext context) {
        this.client = client;
        this.context = context;
    }

    public <T> Optional<T> post(URI path, Object body, Class<T> responseType, String... headers) {
        if (headers.length % 2 == 1) {
            log.error("Headers should be sent in key value pairs. Got this: {}", String.join(", ", headers));
            return Optional.empty();
        }
        HttpPost request = new HttpPost(path);
        Arrays.stream(headers)
            .gather(Gatherers.windowFixed(2))
            .map(pair -> new BasicHeader(pair.get(0), pair.get(1)))
            .forEach(request::setHeader);

        try {
            String bodyStr = mapper.writeValueAsString(body);
            request.setEntity(new StringEntity(bodyStr));
            request.setHeader("Content-Type", "application/json");
            HttpResponse response = client.execute(request, context);
            if (isErrored(response)) {
                return Optional.empty();
            }
            String entityStr = EntityUtils.toString(response.getEntity());
            return responseType == null ? Optional.empty() : Optional.ofNullable(mapper.readValue(entityStr, responseType));
        } catch (JsonProcessingException e) {
            log.warn("Error writing body to str: ", e);
        } catch (UnsupportedEncodingException e) {
            log.warn("Error encoding body: ", e);
        } catch (IOException e) {
            log.warn("Error sending request: ", e);
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
