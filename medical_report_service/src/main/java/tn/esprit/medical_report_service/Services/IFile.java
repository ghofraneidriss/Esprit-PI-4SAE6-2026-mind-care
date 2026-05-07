package tn.esprit.medical_report_service.Services;

import tn.esprit.medical_report_service.Enteties.File;
import java.util.List;

public interface IFile {
    File addFile(File file);

    File updateFile(File file);

    void deleteFile(Long id);

    File getFileById(Long id);

    List<File> getAllFiles();
}
