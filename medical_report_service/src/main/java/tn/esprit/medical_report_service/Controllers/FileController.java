package tn.esprit.medical_report_service.Controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medical_report_service.Enteties.File;
import tn.esprit.medical_report_service.Services.IFile;
import java.util.List;

@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
@RestController
@RequestMapping("/api/files")
@AllArgsConstructor
public class FileController {

    private IFile fileService;

    @PostMapping
    public File addFile(@RequestBody File file) {
        return fileService.addFile(file);
    }

    @PutMapping
    public File updateFile(@RequestBody File file) {
        return fileService.updateFile(file);
    }

    @DeleteMapping("/{id}")
    public void deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
    }

    @GetMapping("/{id}")
    public File getFileById(@PathVariable Long id) {
        return fileService.getFileById(id);
    }

    @GetMapping
    public List<File> getAllFiles() {
        return fileService.getAllFiles();
    }
}
