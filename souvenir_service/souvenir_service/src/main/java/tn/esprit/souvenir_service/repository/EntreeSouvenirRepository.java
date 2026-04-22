package tn.esprit.souvenir_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.souvenir_service.entity.EntreeSouvenir;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;

import java.util.List;

public interface EntreeSouvenirRepository extends JpaRepository<EntreeSouvenir, Long> {

    List<EntreeSouvenir> findByPatientIdOrderByCreatedAtAsc(Long patientId);

    List<EntreeSouvenir> findByPatientIdAndThemeCulturelOrderByCreatedAtAsc(Long patientId, ThemeCulturel themeCulturel);

    List<EntreeSouvenir> findByPatientIdAndMediaTypeOrderByCreatedAtAsc(Long patientId, MediaType mediaType);

    List<EntreeSouvenir> findByPatientIdAndThemeCulturelAndMediaTypeOrderByCreatedAtAsc(
            Long patientId,
            ThemeCulturel themeCulturel,
            MediaType mediaType
    );

    List<EntreeSouvenir> findByThemeCulturelOrderByCreatedAtDesc(ThemeCulturel themeCulturel);

    long countByPatientId(Long patientId);
}
