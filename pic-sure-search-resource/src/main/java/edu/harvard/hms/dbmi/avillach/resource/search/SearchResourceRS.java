package edu.harvard.hms.dbmi.avillach.resource.search;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.domain.*;
import edu.harvard.dbmi.avillach.service.IResourceRS;
import edu.harvard.dbmi.avillach.service.ResourceWebClient;
import edu.harvard.dbmi.avillach.util.exception.ProtocolException;

@Path("/")
@Produces("application/json")
@Consumes("application/json")
@Singleton
public class SearchResourceRS implements IResourceRS {

//	private static final String BEARER_STRING = "Bearer ";
//
//	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(SearchResourceRS.class);

//	@Inject
//	private ApplicationProperties properties;
//	@Inject
//	private HttpClient httpClient;
	
	@Inject
	ResourceRepository resourceRepo;

	@Inject
	ResourceWebClient resourceWebClient;
	
	/**
	 * this store the merged ontologies from the backing resources 
	 */
	private static Map<String, SearchColumnMeta> mergedPhenotypeOntologies;
	
	private static Map<String, SearchColumnMeta> mergedInfoStoreColumns;
	
	private static Thread updateThread = null;

	public SearchResourceRS() {
		logger.debug("default constructor called");
		startUpdateThread();
	}

	private void startUpdateThread() {
		
		if(updateThread == null) {
			updateThread = new Thread(new Runnable() {
	
				@Override
				public void run() {
					//we need to let the object finish construction before referenceing auto-injected fields
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
					} 
					
					while(true) {
						updateOntologies();
						
						//sleep for 1 hour by default.  TODO: set this in standalone.xml
						try {
							Thread.sleep(60 * 60 * 1000);
						} catch (InterruptedException e) {
						}
					}
				}
				
			});
			updateThread.start();
		}
	}

	@Inject
	public SearchResourceRS(ResourceRepository resourceRepo, ResourceWebClient resourceWebClient) {

		this.resourceRepo = resourceRepo;
		this.resourceWebClient = resourceWebClient;
		
		logger.debug("Two param constructor called");
		startUpdateThread();
	}

	@POST
	@Path("/info")
	public ResourceInfo info(QueryRequest infoRequest) {
		ResourceInfo info = new ResourceInfo();
		info.setName("Search Resource - no queries accepted");
		info.setId(UUID.randomUUID());
		
		return info;
	}

	@POST
	@Path("/query")
	public QueryStatus query(QueryRequest queryRequest) {
		logger.debug("Calling Search Resource query()");
		throw new UnsupportedOperationException("Query is not implemented in this resource.");
	}

	@POST
	@Path("/query/{resourceQueryId}/result")
	public Response queryResult(@PathParam("resourceQueryId") String queryId, QueryRequest resultRequest) {
		logger.debug("Calling Search Resource queryResult()");
		throw new UnsupportedOperationException("Query result is not implemented in this resource.");
	}

	@POST
	@Path("/query/{resourceQueryId}/status")
	public QueryStatus queryStatus(@PathParam("resourceQueryId") String queryId, QueryRequest statusRequest) {
		logger.debug("Calling Search Resource queryStatus()");
		throw new UnsupportedOperationException("Query status is not implemented in this resource.");
	}

	@POST
	@Path("/query/sync")
	@Override
	public Response querySync(QueryRequest queryRequest) {
		logger.debug("Calling Search Resource querySync()");
		throw new UnsupportedOperationException("Query sync is not implemented in this resource.");
	}

	@POST
	@Path("/search")
	public SearchResults search(QueryRequest searchRequest) {
		
		//pheno values
		Map<String, SearchColumnMeta> phenotypeResults = searchRequest.getQuery()!=null ? 
			mergedPhenotypeOntologies.entrySet().stream().filter((entry)->{
				String lowerCaseSearchTerm = searchRequest.getQuery().toString().toLowerCase();
				return entry.getKey().toLowerCase().contains(lowerCaseSearchTerm) 
					||(
					entry.getValue().isCategorical() 
					&& 
					entry.getValue().getCategoryValues().stream().map(String::toLowerCase).collect(Collectors.toList())
					.contains(lowerCaseSearchTerm));
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue)) 
		: mergedPhenotypeOntologies;

		// Info Values
		Map<String, SearchColumnMeta> infoResults = searchRequest.getQuery()!=null ? 
			mergedInfoStoreColumns.entrySet().stream().filter((entry)->{
				String lowerCaseSearchTerm = searchRequest.getQuery().toString().toLowerCase();
				return entry.getKey().toLowerCase().contains(lowerCaseSearchTerm) 
					||(
					entry.getValue().isCategorical() 
					&& 
					entry.getValue().getCategoryValues().stream().map(String::toLowerCase).collect(Collectors.toList())
					.contains(lowerCaseSearchTerm));
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue))
		: mergedInfoStoreColumns;
					
		
		return new SearchResults().setResults(
				ImmutableMap.of("phenotypes",phenotypeResults, /*"genes", resultMap,*/ "info", infoResults))
				.setSearchQuery(searchRequest.getQuery().toString());
		
	}

	
	private void updateOntologies() {
	
		ConcurrentHashMap<String, SearchColumnMeta> newPhenotypes = new ConcurrentHashMap<String, SearchColumnMeta>();
		ConcurrentHashMap<String, SearchColumnMeta> newInfoColumns = new ConcurrentHashMap<String, SearchColumnMeta>();
		
		resourceRepo.list().parallelStream().forEach(resource -> {
			//filter out the search resource itself - use a flag for clarity
			if(resource.getHidden()) {
				logger.debug("skipping update of hidden resource " + resource.getName());
				return;
			}
			logger.debug("Updating ontology for resource " + resource.getName());
			
			try {
				QueryRequest queryReq = new QueryRequest();
				queryReq.setQuery(""); //empty search should return all results
				SearchResults search = resourceWebClient.search(resource.getResourceRSPath(), queryReq);
				Map<String, Object> resourceResults = (Map<String, Object>)search.getResults();
					
				//Pheno results
				Map<String, Map<String, Object>> phenoResults = (Map<String, Map<String, Object>>) resourceResults.get("phenotypes");
				logger.debug("found " + phenoResults.size() + " pheno results for " + resource.getName());
				phenoResults.entrySet().stream().forEach(entry -> {
					//merge the metadata fields (max/min, concept values, etc.)
					SearchColumnMeta conceptMeta = updatePhenoMetaData(entry.getValue(), newPhenotypes.get(entry.getKey()), resource.getName());
					
					if(conceptMeta != null) {
						conceptMeta.getResourceAvailability().add(resource.getName());
					}
					newPhenotypes.put(entry.getKey(), conceptMeta);
				});
				
				
				//InfoColumns
				Map<String, Map> infoResults = (Map<String, Map>) resourceResults.get("info");
				logger.debug("found " + infoResults.size() + " infoResults for " + resource.getName());
				infoResults.entrySet().stream().forEach(entry -> {
					//merge the metadata fields (max/min, concept values, etc.)
					SearchColumnMeta conceptMeta = updateInfoMetaData(entry, newInfoColumns.get(entry.getKey()), resource.getName());
					
					if(conceptMeta != null) {
						conceptMeta.getResourceAvailability().add(resource.getName());
					}
					newInfoColumns.put(entry.getKey(), conceptMeta);
				});
			} catch (ProtocolException | NullPointerException e) {
				logger.warn("Could not update resource : " + resource.getName(), e);
				return;
			}
			
			logger.debug("finished updating ontology for resource " + resource.getName());
		});
		
		mergedPhenotypeOntologies = newPhenotypes;
		mergedInfoStoreColumns = newInfoColumns;
		
		//if we are debugging, lets print a list of concepts unique to each institution
		if(logger.isDebugEnabled()) {
			logger.info("Listing Singleton phenotype concepts");
			mergedPhenotypeOntologies.entrySet().stream().forEach(entry -> {
				if(entry.getValue().getResourceAvailability().size() == 1) {
					logger.debug( Arrays.deepToString(entry.getValue().getResourceAvailability().toArray()) + "  " + entry.getKey());
				}
			});
			
			logger.info("Listing Singleton info columns");
			mergedInfoStoreColumns.entrySet().stream().forEach(entry -> {
				if(entry.getValue().getResourceAvailability().size() == 1) {
					logger.debug( Arrays.deepToString(entry.getValue().getResourceAvailability().toArray()) + "  " + entry.getKey());
				}
			});
		}
	}
	
	private SearchColumnMeta updateInfoMetaData(Entry mapEntry, SearchColumnMeta searchColumnMeta, String resourceName) {
		//String - > Object
		Map value = (Map) mapEntry.getValue();
		if(value == null) {
			return searchColumnMeta;
		}
		
		if(searchColumnMeta == null) {
			searchColumnMeta = new SearchColumnMeta();
		}
		
		//"description", "values", "continuous"
		if(value.containsKey("description")) {
			if ( searchColumnMeta.getDescription() == null || searchColumnMeta.getDescription().isEmpty()) {
				searchColumnMeta.setDescription((String) value.get("description"));
			} else {
				if( !value.get("description").equals(searchColumnMeta.getDescription()) ) {
					logger.warn("Conflicting descriptions in info column " + mapEntry.getKey() + " from resource " + resourceName
					+ " already have description from " + Arrays.deepToString(searchColumnMeta.getResourceAvailability().toArray()));
				}
			}
		}

//		this is a bit weird because pheotype and genotype columns use inverted values "continuous"  vs. "Categorical"
		//we are sharing a datatype so we just use 'categorical'
		if(value.containsKey("continuous")) {
			if ( searchColumnMeta.isCategorical() == null) {
				searchColumnMeta.setCategorical( ! ((Boolean)value.get("continuous")));
			} else {
				
				//validate using NOT XOR (since the values are flipped; same value means disagreement)
				if( ! ((Boolean)value.get("continuous") ^ searchColumnMeta.isCategorical()) ) {
					logger.warn("Conflicting 'continuous' flags in info column " + mapEntry.getKey() + " from resource " + resourceName
					+ " already have flag from " + Arrays.deepToString(searchColumnMeta.getResourceAvailability().toArray()));
				
					//if we are confused about the categorical/numeric nature of this column, don't go farther
					return searchColumnMeta;
				}
			}
		}
			
		if(value.containsKey("values")) {
			if ( searchColumnMeta.getCategoryValues() == null) {
				//hashset enforces uniqueness, so we don't need to worry about comparison
				searchColumnMeta.setCategoryValues( (List<String>)value.get("values"));
			} else {
				searchColumnMeta.getCategoryValues().addAll((List<String>)value.get("values"));
			}
		}
		
		searchColumnMeta.getResourceAvailability().add(resourceName);
		return searchColumnMeta;
	}

	/**
	 * compare two sets of concept meta data and return a set of ranges or values encompassing all values
	 * @param conceptMeta
	 * @param value
	 * @return
	 */
	private SearchColumnMeta updatePhenoMetaData(Map<String, Object> conceptMeta, SearchColumnMeta searchColumnMeta, String resourceName) {

		if(conceptMeta == null) {
			return searchColumnMeta;
		}
		
		if(searchColumnMeta == null) {
			searchColumnMeta = new SearchColumnMeta();
		}
		
		if(searchColumnMeta.isCategorical() == null) {
			searchColumnMeta.setCategorical((Boolean)conceptMeta.get("categorical"));
		} else if ( ! searchColumnMeta.isCategorical().equals( (Boolean)conceptMeta.get("categorical"))){
			//for this boolean, don't update, just log a warning
			logger.warn("Conflicting 'categorical' flags in phenotype concept " + conceptMeta.get("name")+ " -- " + resourceName
			+ ": " + (Boolean)conceptMeta.get("categorical") + " " + Arrays.deepToString(searchColumnMeta.getResourceAvailability().toArray()) + ": " + searchColumnMeta.isCategorical());
			//if we are confused about the categorical/numeric nature of this column, don't go farther
			return searchColumnMeta;
		}
		
		if(searchColumnMeta.isCategorical()) {
			if ( searchColumnMeta.getCategoryValues() == null) {
				//hashset enforces uniqueness, so we don't need to worry about comparison
				searchColumnMeta.setCategoryValues( (List)conceptMeta.get("categoryValues"));
			} else {
				searchColumnMeta.getCategoryValues().addAll((List)conceptMeta.get("categoryValues"));
			}
		} else {
		
			if ( searchColumnMeta.getMin() == null || searchColumnMeta.getMin() > (double)conceptMeta.get("min")) {
				searchColumnMeta.setMin( (double)conceptMeta.get("min"));
			}
			
			if ( searchColumnMeta.getMax() == null || searchColumnMeta.getMax() < (double)conceptMeta.get("max")) {
				searchColumnMeta.setMax( (double)conceptMeta.get("max"));
			}
		}
		
		searchColumnMeta.getResourceAvailability().add(resourceName);
		return searchColumnMeta;
	}
}
