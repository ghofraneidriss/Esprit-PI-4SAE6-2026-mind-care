package tn.esprit.followup_alert_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
	"eureka.client.enabled=false",
	"eureka.client.register-with-eureka=false",
	"eureka.client.fetch-registry=false"
})
@ActiveProfiles("test")
class FollowupAlertServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
