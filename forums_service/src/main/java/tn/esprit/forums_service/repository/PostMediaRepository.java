package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.PostMedia;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {

    List<PostMedia> findByPost_IdOrderBySortOrderAsc(Long postId);

    Optional<PostMedia> findFirstByPost_IdOrderBySortOrderAsc(Long postId);

    boolean existsByPost_Id(Long postId);

    Optional<PostMedia> findByIdAndPost_Id(Long id, Long postId);

    @Query("SELECT m.post.id, COUNT(m) FROM PostMedia m WHERE m.post.id IN :ids GROUP BY m.post.id")
    List<Object[]> countByPostIdIn(@Param("ids") Collection<Long> ids);
}
