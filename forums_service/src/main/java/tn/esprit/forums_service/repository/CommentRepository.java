package tn.esprit.forums_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.Comment;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_Id(Long postId);

    long countByPost_Id(Long postId);

    /** Batch comment counts (avoids N+1 when listing many posts). */
    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :ids GROUP BY c.post.id")
    List<Object[]> countByPostIdIn(@Param("ids") Collection<Long> ids);

    /** Comments on posts written by the given user (doctor's threads). */
    @Query("SELECT c FROM Comment c JOIN FETCH c.post p WHERE p.userId = :authorId ORDER BY c.createdAt DESC")
    List<Comment> findByPostAuthorUserId(@Param("authorId") Long authorId);

    @Query("SELECT COUNT(c) FROM Comment c")
    long countCommentsTotal();

    @Query("SELECT COUNT(c) FROM Comment c JOIN c.post p WHERE p.userId = :authorId")
    long countCommentsForPostsByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post p WHERE (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "ORDER BY c.likeCount DESC, c.createdAt DESC")
    List<Comment> findTopForPublicForum(Pageable pageable);
}
