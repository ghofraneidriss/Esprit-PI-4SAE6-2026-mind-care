package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.CommentLike;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByUserIdAndComment_Id(Long userId, Long commentId);

    void deleteByUserIdAndComment_Id(Long userId, Long commentId);

    long countByComment_Id(Long commentId);
}
