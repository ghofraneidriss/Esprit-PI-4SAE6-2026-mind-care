package tn.esprit.medical_report_service.Services;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    public String uploadPdf(byte[] pdfBytes, String fileName, String folder, String tags) {
        try {
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(pdfBytes, Map.of(
                    "resource_type", "raw",
                    "public_id", fileName.replace(".pdf", ""),
                    "folder", folder,
                    "format", "pdf",
                    "tags", tags));
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }
}
