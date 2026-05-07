package tn.esprit.medical_report_service.Controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medical_report_service.DTOs.MRIScanDTO;
import tn.esprit.medical_report_service.DTOs.AIResultDTO;
import tn.esprit.medical_report_service.Services.IMRIScan;

@RestController
@RequestMapping("/api/mri-scans")
@AllArgsConstructor
@CrossOrigin("*")
public class MRIScanController {

    private final IMRIScan mriScanService;

    @PostMapping
    public ResponseEntity<MRIScanDTO> addMRIScan(@Valid @RequestBody MRIScanDTO mriScanDTO) {
        if (mriScanDTO.getMedicalReportId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(mriScanService.addMRIScan(mriScanDTO, mriScanDTO.getMedicalReportId()),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MRIScanDTO> updateMRIScan(@PathVariable Long id, @Valid @RequestBody MRIScanDTO mriScanDTO) {
        return new ResponseEntity<>(mriScanService.updateMRIScan(id, mriScanDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMRIScan(@PathVariable Long id) {
        mriScanService.deleteMRIScan(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MRIScanDTO> getMRIScanById(@PathVariable Long id) {
        return new ResponseEntity<>(mriScanService.getMRIScanById(id), HttpStatus.OK);
    }

    @GetMapping(params = "patientId")
    public ResponseEntity<Page<MRIScanDTO>> getMRIScansByPatientId(@RequestParam Long patientId, Pageable pageable) {
        return new ResponseEntity<>(mriScanService.getMRIScansByPatientId(patientId, pageable), HttpStatus.OK);
    }

    @GetMapping("/{id}/ai-result")
    public ResponseEntity<AIResultDTO> getAIResultByScanId(@PathVariable Long id) {
        MRIScanDTO scan = mriScanService.getMRIScanById(id);
        if (scan.getAiResult() != null) {
            return new ResponseEntity<>(scan.getAiResult(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
