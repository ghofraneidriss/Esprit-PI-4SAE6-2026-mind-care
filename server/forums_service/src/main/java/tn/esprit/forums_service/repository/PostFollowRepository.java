package tn.esprit.forums_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.entity.PostFollow;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostFollowRepository extends JpaRepository<PostFollow, Long> {

    boolean existsByUserIdAndPost_Id(Long userId, Long postId);

    void deleteByUserIdAndPost_Id(Long userId, Long postId);

    List<PostFollow> findByPost_Id(Long postId);

    List<PostFollow> findByUserIdAndPost_IdIn(Long userId, Collection<Long> postIds);
}
