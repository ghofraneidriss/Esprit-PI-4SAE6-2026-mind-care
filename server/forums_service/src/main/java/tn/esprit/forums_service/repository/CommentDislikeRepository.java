package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.CommentDislike;

@Repository
public interface CommentDislikeRepository extends JpaRepository<CommentDislike, Long> {

    boolean existsByUserIdAndComment_Id(Long userId, Long commentId);

    void deleteByUserIdAndComment_Id(Long userId, Long commentId);
}
