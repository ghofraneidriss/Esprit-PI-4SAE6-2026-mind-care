package tn.esprit.medical_report_service.Config;

import com.google.cloud.NoCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

@Configuration
public class GcsConfig {

    @Value("${gcs.project-id}")
    private String projectId;

    @Value("${gcs.credentials.path:}")
    private Resource credentialsResource;

    @Bean
    public Storage storage() throws Exception {
        StorageOptions.Builder storageOptions = StorageOptions.newBuilder()
                .setProjectId(projectId);

        if (credentialsResource != null && StringUtils.hasText(credentialsResource.getFilename())) {
            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(credentialsResource.getInputStream());
            storageOptions.setCredentials(credentials);
        } else {
            storageOptions.setCredentials(NoCredentials.getInstance());
        }

        return storageOptions
                .build()
                .getService();
    }
}
