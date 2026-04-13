package tn.esprit.microservice.inscriptionms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.inscriptionms.entity.Inscription;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
}
