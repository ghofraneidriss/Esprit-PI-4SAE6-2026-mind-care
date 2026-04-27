package tn.esprit.activities_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.activities_service.entity.PhotoActivity;

import java.util.List;

@Repository
public interface PhotoActivityRepository extends JpaRepository<PhotoActivity, Long> {
    
    List<PhotoActivity> findByDifficulty(String difficulty);
    
    @Query("SELECT p FROM PhotoActivity p WHERE p.title LIKE CONCAT('%', :title, '%')")
    List<PhotoActivity> findByTitleContaining(String title);
    
    @Query("SELECT p FROM PhotoActivity p ORDER BY p.createdAt DESC")
    List<PhotoActivity> findAllOrderByCreatedAtDesc();
}
