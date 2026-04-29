package tn.esprit.ordonnance_et_medicaments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;

import java.util.List;

/**
 * Repository pour l'entité Medicine.
 */
@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    
    // Recherche par nom commercial ou DCI pour faciliter la prescription
    List<Medicine> findByCommercialNameContainingIgnoreCaseOrInnContainingIgnoreCase(String name, String inn);

    // Recherche par famille thérapeutique
    List<Medicine> findByTherapeuticFamilyContainingIgnoreCase(String family);

    // Recherche exacte via JPQL pour l'intégrité métier
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Medicine m WHERE LOWER(m.commercialName) = LOWER(:name) AND LOWER(m.inn) = LOWER(:inn)")
    java.util.Optional<Medicine> findExactMatchJPQL(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("inn") String inn);

    // Vérification de doublons via JPQL pour l'intégrité métier
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM Medicine m WHERE LOWER(m.commercialName) = LOWER(:name) AND LOWER(m.inn) = LOWER(:inn)")
    long countByNameAndInnJPQL(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("inn") String inn);

    // Vérification de doublons pour l'importation (Gère la contrainte d'unicité métier)
    boolean existsByCommercialNameIgnoreCaseAndInnIgnoreCase(String commercialName, String inn);

    // JPQL pour la suggestion de médicaments (Autocomplete)
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.commercialName FROM Medicine m WHERE LOWER(m.commercialName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctCommercialNamesByQuery(@org.springframework.data.repository.query.Param("query") String query);

    // JPQL pour la suggestion de catégories/familles (Autocomplete)
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m.therapeuticFamily FROM Medicine m WHERE LOWER(m.therapeuticFamily) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<String> findDistinctTherapeuticFamiliesByQuery(@org.springframework.data.repository.query.Param("query") String query);
}
