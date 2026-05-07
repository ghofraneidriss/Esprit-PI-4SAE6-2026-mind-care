package tn.esprit.medical_report_service.Services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.medical_report_service.Enteties.File;
import tn.esprit.medical_report_service.Repositories.FileRepository;
import java.util.List;

@Service
@AllArgsConstructor
public class FileService implements IFile {

    private FileRepository fileRepository;

    @Override
    public File addFile(File file) {
        return fileRepository.save(file);
    }

    @Override
    public File updateFile(File file) {
        return fileRepository.save(file);
    }

    @Override
    public void deleteFile(Long id) {
        fileRepository.deleteById(id);
    }

    @Override
    public File getFileById(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    @Override
    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }
}
