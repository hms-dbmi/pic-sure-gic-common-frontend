package edu.harvard.hms.avillach.passthru;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.net.ssl.SSLContext;

@SpringBootTest
class PassthruApplicationTests {

    @MockitoBean
    SSLContext context;

	@Test
	void contextLoads() {
	}

}
