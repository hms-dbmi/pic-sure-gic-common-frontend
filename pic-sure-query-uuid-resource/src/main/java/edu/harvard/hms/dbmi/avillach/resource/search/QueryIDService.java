package edu.harvard.hms.dbmi.avillach.resource.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.dbmi.avillach.data.entity.Query;
import edu.harvard.dbmi.avillach.data.entity.Resource;
import edu.harvard.dbmi.avillach.data.repository.QueryRepository;
import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.domain.QueryRequest;
import edu.harvard.dbmi.avillach.domain.QueryStatus;
import edu.harvard.dbmi.avillach.util.PicSureStatus;
import edu.harvard.dbmi.avillach.util.exception.ProtocolException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@ApplicationScoped
public class QueryIDService {


    @Inject
    QueryRepository queryRepository;

    @Inject
    ResourceRepository resourceRepository;


    public Optional<QueryStatus> createCommonAreaQuery(QueryRequest request) {

        Resource resource = resourceRepository.getById(request.getResourceUUID());
        String queryJson = getQueryJson(request);

        if (resource == null || queryJson == null) {
            return Optional.empty();
        }

        Query query = new Query();
        query.setResource(resource);
        query.setStatus(PicSureStatus.ERROR);
        query.setQuery(queryJson);

        queryRepository.persist(query);

        if (query.getUuid() == null) {
            return Optional.empty();
        }

        QueryStatus queryStatus = new QueryStatus();
        queryStatus.setResourceID(resource.getUuid());
        queryStatus.setPicsureResultId(query.getUuid());
        queryStatus.setResultMetadata(new HashMap<>());

        return Optional.of(queryStatus);
    }

    private String getQueryJson(QueryRequest request) {
        if (request == null) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
