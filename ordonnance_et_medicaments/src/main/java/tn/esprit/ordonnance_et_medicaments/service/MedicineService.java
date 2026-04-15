package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Service pour la gestion des médicaments (Medicine).
 */
@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class MedicineService {

    private final MedicineRepository medicineRepository;

    /**
     * Importation massive de médicaments depuis un fichier Excel (XLSX) ou CSV.
     * @param file Le fichier Multipart envoyé par le front.
     * @return Un message récapitulatif de l'opération.
     */
    public String importMedicines(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
            return importFromCSV(file);
        } else {
            return importFromExcel(file);
        }
    }

    private String importFromExcel(MultipartFile file) {
        int countSuccess = 0;
        int countDuplicate = 0;
        int countError = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Sauter l'entête (Header)
            if (rows.hasNext()) rows.next();

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                try {
                    String name = getCellValueAsString(currentRow.getCell(0));
                    String inn = getCellValueAsString(currentRow.getCell(1));
                    String category = currentRow.getLastCellNum() > 2 ? getCellValueAsString(currentRow.getCell(2)) : "Other";
                    String contra = currentRow.getLastCellNum() > 3 ? getCellValueAsString(currentRow.getCell(3)) : "None";

                    if (processMedicine(name, inn, category, contra)) {
                        countSuccess++;
                    } else {
                        countDuplicate++;
                    }
                } catch (Exception e) {
                    countError++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to store excel data: " + e.getMessage());
        }

        return String.format("Excel Import completed: %d success, %d duplicates skipped, %d errors.", 
                             countSuccess, countDuplicate, countError);
    }

    private String importFromCSV(MultipartFile file) {
        int countSuccess = 0;
        int countDuplicate = 0;
        int countError = 0;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                if (firstLine) { 
                    // Supprimer le BOM UTF-8 s'il existe
                    if (line.startsWith("\uFEFF")) line = line.substring(1);
                    firstLine = false; 
                    continue; 
                }

                try {
                    // Détection intelligente du séparateur (Semicolon, Comma or Tab)
                    String separator = line.contains(";") ? ";" : (line.contains(",") ? "," : "\t");
                    // Regex sophistiquée pour spliter en ignorant les virgules entre guillemets
                    String[] data = line.split(separator + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    if (data.length < 2) { 
                        System.out.println("Line " + lineNumber + " skipped: not enough columns.");
                        countError++; 
                        continue; 
                    }

                    String name = cleanCSVValue(data[0]);
                    String inn = cleanCSVValue(data[1]);
                    String category = data.length > 2 ? cleanCSVValue(data[2]) : "Other";
                    String contra = data.length > 3 ? cleanCSVValue(data[3]) : "None";

                    System.out.println("Processing Line " + lineNumber + ": " + name + " | " + inn);

                    if (processMedicine(name, inn, category, contra)) {
                        countSuccess++;
                    } else {
                        System.out.println("Line " + lineNumber + " skipped: Duplicate detected.");
                        countDuplicate++;
                    }
                } catch (Exception e) {
                    System.err.println("Error at line " + lineNumber + ": " + e.getMessage());
                    countError++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to store CSV data: " + e.getMessage());
        }

        return String.format("CSV Import completed: %d items added, %d duplicates ignored, %d failed lines.", 
                             countSuccess, countDuplicate, countError);
    }

    private String cleanCSVValue(String value) {
        if (value == null) return "";
        return value.trim().replace("\"", "");
    }

    private boolean processMedicine(String name, String inn, String category, String contra) {
        if (name.isEmpty() || inn.isEmpty()) return false;

        // Vigilance anti-doublon via JPQL pour l'intégrité métier
        if (medicineRepository.countByNameAndInnJPQL(name, inn) > 0) {
            return false;
        }

        Medicine medicine = new Medicine();
        medicine.setCommercialName(name);
        medicine.setInn(inn);
        medicine.setTherapeuticFamily(category.isEmpty() ? "Other" : category);
        medicine.setContraindications(contra.isEmpty() ? "None" : contra);

        medicineRepository.save(medicine);
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }

    // Pour l'ADMIN : Créer/Sauvegarder un médicament avec vérification par JPQL
    public String saveMedicine(Medicine medicine) {
        if (medicineRepository.countByNameAndInnJPQL(medicine.getCommercialName(), medicine.getInn()) > 0) {
            return "Medicine '" + medicine.getCommercialName() + " (" + medicine.getInn() + ")' already exists in the catalog.";
        }
        medicineRepository.save(medicine);
        return "Medicine '" + medicine.getCommercialName() + "' has been successfully added to the catalog.";
    }

    // Pour l'ADMIN : Modifier un médicament
    public Medicine updateMedicine(Long id, Medicine details) {
        Medicine m = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
        m.setCommercialName(details.getCommercialName());
        m.setInn(details.getInn());
        m.setTherapeuticFamily(details.getTherapeuticFamily());
        m.setContraindications(details.getContraindications());
        return medicineRepository.save(m);
    }

    // Pour l'ADMIN : Supprimer un médicament
    public void deleteMedicine(Long id) {
        medicineRepository.deleteById(id);
    }

    // Pour le MEDECIN & ADMIN : Récupérer tout ou rechercher
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    public List<Medicine> searchMedicines(String query) {
        return medicineRepository.findByCommercialNameContainingIgnoreCaseOrInnContainingIgnoreCase(query, query);
    }

    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
    }

    // Autocomplete methods
    public List<String> suggestCommercialNames(String query) {
        return medicineRepository.findDistinctCommercialNamesByQuery(query);
    }

    public List<String> suggestTherapeuticFamilies(String query) {
        return medicineRepository.findDistinctTherapeuticFamiliesByQuery(query);
    }
}
