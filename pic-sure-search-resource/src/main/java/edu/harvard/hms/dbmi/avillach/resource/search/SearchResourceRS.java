package edu.harvard.hms.dbmi.avillach.resource.search;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.ejb.Singleton;
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
import edu.harvard.dbmi.avillach.util.exception.ResourceInterfaceException;

@Path("/")
@Produces("application/json")
@Consumes("application/json")
@Singleton
public class SearchResourceRS implements IResourceRS {

	private static final Logger logger = LoggerFactory.getLogger(SearchResourceRS.class);
	
	/*
	 * these store the merged ontologies from the backing resources 
	 */
	private static Map<String, SearchColumnMeta> mergedPhenotypeOntologies;
	
	private static Map<String, SearchColumnMeta> mergedInfoStoreColumns;
	

	public SearchResourceRS() {
		logger.debug("default constructor called");
	}

	@POST
	@Path("/info")
	public ResourceInfo info(QueryRequest infoRequest) {
		logger.debug("Calling Search Resource info()");
		throw new UnsupportedOperationException("Info is not implemented in this resource.");
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

	/**
	 * Only support queries of type 'INFO_COLUMN_LISTING'
	 */
	@POST
	@Path("/query/sync")
	@Override
	public Response querySync(QueryRequest queryRequest) {
		logger.debug("Calling Search Resource querySync()");
	
		String expectedResultType = ((Map)queryRequest.getQuery()).get("expectedResultType").toString();
		
		if(expectedResultType.equals("INFO_COLUMN_LISTING")){
			ArrayList<Map> infoStores = new ArrayList<>();
			mergedInfoStoreColumns.entrySet().stream().forEach((entry)->{
				infoStores.add(ImmutableMap.of(
						"key", entry.getKey(), 
						"description", entry.getValue().getDescription(), 
						"isContinuous", !entry.getValue().isCategorical()));
			});
			return Response.ok(infoStores, "application/json").build();
		}
		
		throw new UnsupportedOperationException("Only INFO_COLUMN_LISTING queries supported by this search resource");
	
	}
	
	/**
	 * Implement this formatting for INFO_COLUMN_LISTING queries (which we do support)
	 */
	@POST
	@Path("/query/format")
	public Response queryFormat(QueryRequest formatRequest) {
		String expectedResultType = ((Map)formatRequest.getQuery()).get("expectedResultType").toString();
		
		if(expectedResultType.equals("INFO_COLUMN_LISTING")){
			return Response.ok().entity("INFO_COLUMN_LISTING query").build();
		} else {
			throw new NotSupportedException("Query formatting is not implemented in this resource.");
		}
	}

	@POST
	@Path("/search")
	public SearchResults search(QueryRequest searchRequest) {
		logger.debug("common search resoruce called " + searchRequest.getQuery() );
		
		if(mergedPhenotypeOntologies == null || mergedInfoStoreColumns == null) {
			logger.warn("Search attempted, but we have no ontology!  Pheno: " + mergedPhenotypeOntologies + "  Info: " + mergedInfoStoreColumns);
			throw new ServiceUnavailableException("No Ontology data available");
		}
		
		
		final String lowerCaseSearchTerm = searchRequest.getQuery().toString().toLowerCase();
		
		//pheno values
		Map<String, SearchColumnMeta> phenotypeResults = searchRequest.getQuery()!=null ? 
			mergedPhenotypeOntologies.entrySet().stream().filter((entry)->{
				return entry.getKey().toLowerCase().contains(lowerCaseSearchTerm) 
					||(
					entry.getValue().isCategorical() 
					&& 
					entry.getValue().getCategoryValues().stream().map(String::toLowerCase).collect(Collectors.toList())
					.contains(lowerCaseSearchTerm));
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue)) 
		: mergedPhenotypeOntologies;

		// Info Values
		final Map<String, SearchColumnMeta> infoResults = searchRequest.getQuery()==null ? mergedInfoStoreColumns : new HashMap<String, SearchColumnMeta>();
		
		if ( searchRequest.getQuery()!=null ) {
			mergedInfoStoreColumns.entrySet().stream().forEach((entry)->{
				if(entry.getKey().toLowerCase().contains(lowerCaseSearchTerm)){
					infoResults.put(entry.getKey(), entry.getValue());
				} else if(entry.getValue().isCategorical()){
					//we have to filter the values that match (INFO only - the UI does this for phenotype concepts)
					// this is info-only logic because there are some info columns with way to many value for the UI to handle
					List<String> matchingTerms = entry.getValue().getCategoryValues().stream().filter((value)->{
						return value.toLowerCase().contains(lowerCaseSearchTerm);
					}).collect(Collectors.toList());
					if(matchingTerms.size() > 0) {
						SearchColumnMeta filteredResultsMeta = new SearchColumnMeta();
						filteredResultsMeta.setName(entry.getKey());
						filteredResultsMeta.setDescription(entry.getValue().getDescription());
						filteredResultsMeta.setCategorical(true);
						filteredResultsMeta.setCategoryValues(new HashSet<String>(matchingTerms));
						infoResults.put(entry.getKey(), filteredResultsMeta);
					}
				}
			});
		}
		
		return new SearchResults().setResults(
				ImmutableMap.of("phenotypes",phenotypeResults, /*"genes", resultMap,*/ "info", infoResults))
				.setSearchQuery(searchRequest.getQuery().toString());
	}

	/**
	 * This method queries the database for valid resources, and retrieves a complete listing of all phenotype and info columns.
	 * These data responses are then combined into a single merged ontology that is used to search for concepts across all resources.
	 * @param resourceRepo
	 * @param resourceWebClient
	 */
	public static void updateOntologies(ResourceRepository resourceRepo, ResourceWebClient resourceWebClient) {
	
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
					newPhenotypes.put(entry.getKey(), conceptMeta);
				});
				
				//InfoColumns
				Map<String, Map> infoResults = (Map<String, Map>) resourceResults.get("info");
				logger.debug("found " + infoResults.size() + " infoResults for " + resource.getName());
				infoResults.entrySet().stream().forEach(entry -> {
					//merge the metadata fields (max/min, concept values, etc.)
					SearchColumnMeta conceptMeta = updateInfoMetaData(entry, newInfoColumns.get(entry.getKey()), resource.getName());
					newInfoColumns.put(entry.getKey(), conceptMeta);
				});
			} catch (Exception e) {
				logger.warn("Could not update resource : " + resource.getName(), e);
				return;
			}
			
			logger.debug("finished updating ontology for resource " + resource.getName());
		});
		
		mergedPhenotypeOntologies = newPhenotypes;
		mergedInfoStoreColumns = newInfoColumns;
		
		//if we are debugging(TRACE level), lets print a list of concepts unique to each institution.  this is a lot of output
		if(logger.isTraceEnabled()) {
			logger.info("Listing Singleton phenotype concepts");
			mergedPhenotypeOntologies.entrySet().stream().forEach(entry -> {
				if(entry.getValue().getResourceAvailability().size() == 1) {
					logger.debug( "Singleton: " + Arrays.deepToString(entry.getValue().getResourceAvailability().toArray()) + "  " + entry.getKey());
				}
			});
			
			logger.info("Listing Singleton info columns");
			mergedInfoStoreColumns.entrySet().stream().forEach(entry -> {
				if(entry.getValue().getResourceAvailability().size() == 1) {
					logger.debug( "Singleton: " + Arrays.deepToString(entry.getValue().getResourceAvailability().toArray()) + "  " + entry.getKey());
				}
			});
		}
	}
	
	private static SearchColumnMeta updateInfoMetaData(Entry mapEntry, SearchColumnMeta searchColumnMeta, String resourceName) {
		//String - > Object
		Map value = (Map) mapEntry.getValue();
		if(value == null) {
			return searchColumnMeta;
		}
		
		if(searchColumnMeta == null) {
			searchColumnMeta = new SearchColumnMeta();
			searchColumnMeta.setName((String)mapEntry.getKey());
		}
		
		//"description", "values", "continuous"
		if(value.containsKey("description")) {
			String descriptionStr = (String) value.get("description");
			//just a little hack to clean up some data
			if(descriptionStr.startsWith("Description=\"")){
				descriptionStr = descriptionStr.substring(13, descriptionStr.length()-1);
			}
			
			if ( searchColumnMeta.getDescription() == null || searchColumnMeta.getDescription().isEmpty()) {
				searchColumnMeta.setDescription(descriptionStr);
			} else {
				if( !descriptionStr.equals(searchColumnMeta.getDescription()) ) {
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
				searchColumnMeta.setCategoryValues(ConcurrentHashMap.newKeySet());
			}
			searchColumnMeta.getCategoryValues().addAll((List<String>)value.get("values"));
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
	private static SearchColumnMeta updatePhenoMetaData(Map<String, Object> conceptMeta, SearchColumnMeta searchColumnMeta, String resourceName) {

		if(conceptMeta == null) {
			return searchColumnMeta;
		}
		
		if(searchColumnMeta == null) {
			searchColumnMeta = new SearchColumnMeta();
			searchColumnMeta.setName((String)conceptMeta.get("name"));
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
				searchColumnMeta.setCategoryValues(ConcurrentHashMap.newKeySet() );
			}
			searchColumnMeta.getCategoryValues().addAll((List)conceptMeta.get("categoryValues"));
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
