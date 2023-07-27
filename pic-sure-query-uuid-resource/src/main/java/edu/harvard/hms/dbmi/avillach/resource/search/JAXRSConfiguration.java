package edu.harvard.hms.dbmi.avillach.resource.search;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.harvard.dbmi.avillach.data.repository.ResourceRepository;
import edu.harvard.dbmi.avillach.service.ResourceWebClient;

@ApplicationPath("")
@WebListener
public class JAXRSConfiguration extends Application implements ServletContextListener {
	
	@Inject
	ResourceRepository resourceRepo;

	@Inject
	ResourceWebClient resourceWebClient;
	
    @Resource(mappedName = "java:global/ontology_update_interval_ms")
    private String ontology_update_interval_str; 
    //6 hour default
  	private long updateIntervalLong = 6 * 60 * 60 * 1000;
  		
    Thread updateThread;
	
	private boolean running = true;
	
	private static final Logger logger = LoggerFactory.getLogger(JAXRSConfiguration.class);

}
