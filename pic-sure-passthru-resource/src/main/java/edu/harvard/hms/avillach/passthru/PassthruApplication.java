package edu.harvard.hms.avillach.passthru;

import edu.harvard.hms.avillach.passthru.remote.RemoteResourceConfig;
import edu.harvard.hms.avillach.passthru.remote.RemoteResourceService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan( {"edu.harvard.hms.avillach.passthru", "edu.harvard.hms.avillach.passthru.remote" })
@SpringBootApplication
public class PassthruApplication {

	public static void main(String[] args) {
		SpringApplication.run(PassthruApplication.class, args);
	}

}
