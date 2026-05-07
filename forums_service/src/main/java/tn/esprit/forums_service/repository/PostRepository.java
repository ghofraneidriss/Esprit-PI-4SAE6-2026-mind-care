package tn.esprit.forums_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.forums_service.dto.PostListProjection;
import tn.esprit.forums_service.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByCategory_Id(Long categoryId);

    @Query("SELECT p FROM Post p WHERE (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "ORDER BY p.createdAt DESC")
    List<Post> findRecentActivePublished(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId AND (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') ORDER BY p.createdAt DESC")
    List<Post> findActivePublishedByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Post p WHERE p.inactive = true ORDER BY p.inactiveSince DESC, p.createdAt DESC")
    List<Post> findInactivePostsHistory(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND COALESCE(p.lastInteractionAt, p.createdAt) < :before")
    List<Post> findPublishedActiveWithLastInteractionBefore(@Param("before") LocalDateTime before);

    long countByInactiveTrue();

    @Query("SELECT p.category.id, p.category.name, COUNT(p) FROM Post p WHERE p.category IS NOT NULL "
            + "GROUP BY p.category.id, p.category.name ORDER BY COUNT(p) DESC")
    List<Object[]> countPostsGroupedByCategory(Pageable pageable);

    List<Post> findByCategoryId(Long categoryId);

    List<Post> findByUserId(Long userId);

    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p.id AS id, p.title AS title, p.userId AS userId, p.createdAt AS createdAt, "
            + "c.id AS categoryId, c.name AS categoryName, p.inactive AS inactive, p.inactiveSince AS inactiveSince, "
            + "p.status AS status "
            + "FROM Post p LEFT JOIN p.category c ORDER BY p.createdAt DESC")
    List<PostListProjection> findStaffListingAll(Pageable pageable);

    @Query("SELECT p.id AS id, p.title AS title, p.userId AS userId, p.createdAt AS createdAt, "
            + "c.id AS categoryId, c.name AS categoryName, p.inactive AS inactive, p.inactiveSince AS inactiveSince, "
            + "p.status AS status "
            + "FROM Post p LEFT JOIN p.category c WHERE p.userId = :authorId ORDER BY p.createdAt DESC")
    List<PostListProjection> findStaffListingByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    long countByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.lastInteractionAt = p.createdAt WHERE p.lastInteractionAt IS NULL")
    int backfillLastInteractionFromCreatedAt();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.status IS NULL OR UPPER(p.status) <> 'DRAFT'")
    long countPublishedPosts();

    @Query("SELECT COUNT(p) FROM Post p WHERE UPPER(p.status) = 'DRAFT'")
    long countDraftPosts();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :uid AND (p.status IS NULL OR UPPER(p.status) <> 'DRAFT')")
    long countPublishedByAuthor(@Param("uid") Long uid);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :uid AND UPPER(p.status) = 'DRAFT'")
    long countDraftByAuthor(@Param("uid") Long uid);

    @Query("SELECT COUNT(p) FROM Post p WHERE (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED')")
    long countPublishedActive();

    @Query("SELECT COUNT(p) FROM Post p WHERE (p.inactive IS NULL OR p.inactive = false) "
            + "AND (p.status IS NULL OR TRIM(p.status) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    long countPublishedActiveFiltered(@Param("categoryId") Long categoryId);

    /**
     * Distinct user ids who either published a public thread or commented on one
     * (MySQL UNION deduplicates).
     */
    @Query(value = "SELECT COUNT(*) FROM ("
            + "SELECT user_id AS uid FROM post WHERE COALESCE(inactive, 0) = 0 "
            + "AND (status IS NULL OR TRIM(IFNULL(status,'')) = '' OR UPPER(TRIM(status)) = 'PUBLISHED') "
            + "UNION "
            + "SELECT c.user_id FROM forum_comment c INNER JOIN post p ON p.id = c.post_id "
            + "WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED')"
            + ") t", nativeQuery = true)
    long countDistinctForumParticipantUserIds();

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByNewest(
            @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "ORDER BY (SELECT COUNT(*) FROM forum_comment c WHERE c.post_id = p.id) DESC, "
            + "COALESCE(p.last_interaction_at, p.created_at) DESC, p.created_at DESC "
            + "LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByCommentCount(
            @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "ORDER BY (SELECT COUNT(*) FROM post_view v WHERE v.post_id = p.id) DESC, "
            + "p.created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByViewCount(
            @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "AND ((LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "OR (LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%'))))", nativeQuery = true)
    long countPublishedActiveSearch(@Param("q") String q, @Param("categoryId") Long categoryId);

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "AND ((LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "OR (LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))) "
            + "ORDER BY p.created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByNewestSearch(
            @Param("q") String q, @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "AND ((LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "OR (LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))) "
            + "ORDER BY (SELECT COUNT(*) FROM forum_comment c WHERE c.post_id = p.id) DESC, "
            + "COALESCE(p.last_interaction_at, p.created_at) DESC, p.created_at DESC "
            + "LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByCommentCountSearch(
            @Param("q") String q, @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value = "SELECT p.id FROM post p WHERE COALESCE(p.inactive, 0) = 0 "
            + "AND (p.status IS NULL OR TRIM(IFNULL(p.status,'')) = '' OR UPPER(TRIM(p.status)) = 'PUBLISHED') "
            + "AND (:categoryId IS NULL OR p.category_id = :categoryId) "
            + "AND ((LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "OR (LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%')))) "
            + "ORDER BY (SELECT COUNT(*) FROM post_view v WHERE v.post_id = p.id) DESC, "
            + "p.created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findPublishedActiveIdsByViewCountSearch(
            @Param("q") String q, @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);
}
