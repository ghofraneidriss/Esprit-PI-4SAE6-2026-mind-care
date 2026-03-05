package tn.esprit.incident_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.incident_service.entity.IncidentType;

@Repository
public interface IncidentTypeRepository extends JpaRepository<IncidentType, Long> {

    // On peut ajouter des méthodes pour filtrer les types si besoin
    // Par défaut, findAll() retournera tous les types
    // Si on veut soft-delete aussi => findAllActive() similaire à IncidentRepository
    
    // Pour simplifier ici, on ne filtre pas les types (Admin only)
}
