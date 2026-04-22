package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.PostRating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRatingRepository extends JpaRepository<PostRating, Long> {

    Optional<PostRating> findByPost_IdAndUserId(Long postId, Long userId);

    @Query("SELECT r.post.id, r.stars FROM PostRating r WHERE r.post.id IN :ids AND r.userId = :userId")
    List<Object[]> findRatingsForUser(@Param("ids") Collection<Long> postIds, @Param("userId") Long userId);

    @Query("SELECT r.post.id, AVG(r.stars), COUNT(r) FROM PostRating r WHERE r.post.id IN :ids GROUP BY r.post.id")
    List<Object[]> avgAndCountByPostIds(@Param("ids") Collection<Long> ids);
}
