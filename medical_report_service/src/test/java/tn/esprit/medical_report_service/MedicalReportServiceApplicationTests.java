package tn.esprit.medical_report_service;

import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class MedicalReportServiceApplicationTests {

    @MockBean
    Storage storage;

    @Test
    void contextLoads() {
    }

}
