package tn.esprit.activities_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.activities_service.entity.PhotoActivity;
import tn.esprit.activities_service.repository.PhotoActivityRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PhotoActivityService {

    @Autowired
    private PhotoActivityRepository photoActivityRepository;

    @Autowired
    private ImageProcessingService imageProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<PhotoActivity> getAllPhotoActivities() {
        return photoActivityRepository.findAll();
    }

    public Optional<PhotoActivity> getPhotoActivityById(Long id) {
        return photoActivityRepository.findById(id);
    }

    /**
     * Chargement pour servir les octets image (évite lazy sans session).
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> getImageBytes(Long id) {
        return photoActivityRepository.findById(id).flatMap(p -> {
            if (!p.isHasStoredImage() || p.getImageData() == null || p.getImageData().length == 0) {
                return Optional.empty();
            }
            return Optional.of(p.getImageData());
        });
    }

    @Transactional(readOnly = true)
    public Optional<String> getImageContentType(Long id) {
        return photoActivityRepository.findById(id).map(PhotoActivity::getImageContentType);
    }

    public Optional<String> getExternalImageUrl(Long id) {
        return photoActivityRepository.findById(id).map(PhotoActivity::getExternalImageUrl);
    }

    public PhotoActivity createWithImage(
            MultipartFile file,
            String title,
            String description,
            String difficulty,
            String correctAnswer,
            String optionsJson
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        byte[] processed = imageProcessingService.resizeAndCompressToJpeg(file.getBytes());
        List<String> options = parseOptions(optionsJson);
        PhotoActivity p = new PhotoActivity();
        p.setTitle(title);
        p.setDescription(description != null ? description : "");
        p.setDifficulty(difficulty != null ? difficulty : "EASY");
        p.setCorrectAnswer(correctAnswer);
        p.setOptions(options);
        p.setImageData(processed);
        p.setImageContentType("image/jpeg");
        p.setHasStoredImage(true);
        // DB column image_url is often NOT NULL; stored images use image_data, keep URL column as empty string
        p.setExternalImageUrl("");
        return photoActivityRepository.save(p);
    }

    public PhotoActivity updatePhotoActivity(Long id, PhotoActivity incoming) {
        return photoActivityRepository.findById(id).map(existing -> {
            existing.setTitle(incoming.getTitle());
            existing.setDescription(incoming.getDescription());
            existing.setDifficulty(incoming.getDifficulty());
            existing.setCorrectAnswer(incoming.getCorrectAnswer());
            if (incoming.getOptions() != null) {
                existing.setOptions(incoming.getOptions());
            }
            return photoActivityRepository.save(existing);
        }).orElse(null);
    }

    public PhotoActivity updateWithImage(
            Long id,
            MultipartFile file,
            String title,
            String description,
            String difficulty,
            String correctAnswer,
            String optionsJson
    ) throws IOException {
        return photoActivityRepository.findById(id).map(existing -> {
            existing.setTitle(title);
            existing.setDescription(description != null ? description : "");
            existing.setDifficulty(difficulty != null ? difficulty : "EASY");
            existing.setCorrectAnswer(correctAnswer);
            existing.setOptions(parseOptions(optionsJson));
            if (file != null && !file.isEmpty()) {
                try {
                    byte[] processed = imageProcessingService.resizeAndCompressToJpeg(file.getBytes());
                    existing.setImageData(processed);
                    existing.setImageContentType("image/jpeg");
                    existing.setHasStoredImage(true);
                    existing.setExternalImageUrl("");
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not process the image file. Please try another JPEG or PNG.", e);
                }
            }
            return photoActivityRepository.save(existing);
        }).orElse(null);
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean deletePhotoActivity(Long id) {
        if (photoActivityRepository.existsById(id)) {
            photoActivityRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<PhotoActivity> getPhotoActivitiesByDifficulty(String difficulty) {
        return photoActivityRepository.findByDifficulty(difficulty);
    }

    public List<PhotoActivity> searchPhotoActivities(String title) {
        return photoActivityRepository.findByTitleContaining(title);
    }
}
