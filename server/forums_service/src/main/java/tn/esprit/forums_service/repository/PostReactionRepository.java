package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.PostReaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPost_IdAndUserId(Long postId, Long userId);

    @Query("SELECT r.post.id, r.reactionType FROM PostReaction r WHERE r.post.id IN :ids AND r.userId = :userId")
    List<Object[]> findReactionsForUser(@Param("ids") Collection<Long> postIds, @Param("userId") Long userId);

    @Query("SELECT r.post.id, r.reactionType, COUNT(r) FROM PostReaction r WHERE r.post.id IN :ids GROUP BY r.post.id, r.reactionType")
    List<Object[]> countGroupedByPostIds(@Param("ids") Collection<Long> ids);

    void deleteByPost_IdAndUserId(Long postId, Long userId);
}
