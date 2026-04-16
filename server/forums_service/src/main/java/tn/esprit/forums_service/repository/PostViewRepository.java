package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.PostView;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    long countByPost_Id(Long postId);

    @Query("SELECT v.post.id, COUNT(v) FROM PostView v WHERE v.post.id IN :ids GROUP BY v.post.id")
    List<Object[]> countByPostIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT COUNT(v) FROM PostView v")
    long countAllViews();

    @Query("SELECT COUNT(v) FROM PostView v JOIN v.post p WHERE p.userId = :authorId")
    long countViewsForPostsByAuthor(@Param("authorId") Long authorId);
}
