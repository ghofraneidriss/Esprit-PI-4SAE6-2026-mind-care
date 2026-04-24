package tn.esprit.medical_report_service.Services;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.concurrent.TimeUnit;
@Service
@RequiredArgsConstructor
public class GcsReportStorageService {

    private final Storage storage;

    @Value("${gcs.bucket}")
    private String bucketName;

    public String uploadPdfAndGetSignedUrl(byte[] pdfBytes, String objectName) {

        BlobId blobId = BlobId.of(bucketName, objectName);

        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/pdf")
                .build();

        storage.create(blobInfo, pdfBytes);

        URL signedUrl = storage.signUrl(
                blobInfo,
                24,
                TimeUnit.HOURS,
                Storage.SignUrlOption.withV4Signature()
        );

        return signedUrl.toString();
    }
}
