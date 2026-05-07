package tn.esprit.medical_report_service.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medical_report_service.Enteties.File;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
}
