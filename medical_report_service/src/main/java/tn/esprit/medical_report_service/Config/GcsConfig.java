package tn.esprit.medical_report_service.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class GcsConfig {

    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.credentials.path}")
    private Resource credentialsResource;

    @Bean
    public Storage storage() throws Exception {
        GoogleCredentials credentials =
                GoogleCredentials.fromStream(credentialsResource.getInputStream());

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}