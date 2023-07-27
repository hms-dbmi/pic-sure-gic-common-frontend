package edu.harvard.hms.dbmi.avillach.resource.search;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import edu.harvard.dbmi.avillach.util.exception.ProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.dbmi.avillach.service.IResourceRS;
import edu.harvard.dbmi.avillach.service.ResourceWebClient;

@Path("/")
@Produces("application/json")
@Consumes("application/json")
@Singleton
public class QueryIDResourceRS implements IResourceRS {
	
	
	private static final String ERROR_MESSAGE =
		"This resource is a shallow implementation of the query endpoint for UUID generation.";
	private static final Logger logger = LoggerFactory.getLogger(QueryIDResourceRS.class);

	@Inject
	QueryIDService queryIDService;

	public QueryIDResourceRS() {
		logger.debug("default constructor called");
	}

	@GET
	@Path("/status")
	public Response status() {
		return Response.ok().build();
	}

	@POST
	@Path("/info")
	public ResourceInfo info(QueryRequest infoRequest) {
		logger.debug("Calling Search Resource info()");
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@POST
	@Path("/query")
	public QueryStatus query(QueryRequest queryRequest) {
		logger.debug("Calling Search Resource query()");
		return queryIDService.createCommonAreaQuery(queryRequest)
			.orElseThrow(() -> new ProtocolException(ProtocolException.INCORRECTLY_FORMATTED_REQUEST));
	}

	@POST
	@Path("/query/{resourceQueryId}/result")
	public Response queryResult(@PathParam("resourceQueryId") String queryId, QueryRequest resultRequest) {
		logger.debug("Calling Search Resource queryResult()");
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@POST
	@Path("/query/{resourceQueryId}/status")
	public QueryStatus queryStatus(@PathParam("resourceQueryId") String queryId, QueryRequest statusRequest) {
		logger.debug("Calling Search Resource queryStatus()");
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@POST
	@Path("/query/sync")
	@Override
	public Response querySync(QueryRequest queryRequest) {
		logger.debug("Calling Search Resource querySync()");
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@POST
	@Path("/query/format")
	public Response queryFormat(QueryRequest formatRequest) {
		logger.debug("Calling Search Resource queryFormat()");
		throw new UnsupportedOperationException(ERROR_MESSAGE);

	}

	@POST
	@Path("/search")
	public SearchResults search(QueryRequest searchRequest) {
		logger.debug("Calling Search Resource search()" );
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}
}
