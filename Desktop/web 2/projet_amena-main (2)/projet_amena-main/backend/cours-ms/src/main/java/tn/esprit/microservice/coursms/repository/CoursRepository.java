package tn.esprit.microservice.coursms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.coursms.entity.Cours;

public interface CoursRepository extends JpaRepository<Cours, Long> {
}
