package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.esprit.forums_service.dto.ForumStatsDto;
import tn.esprit.forums_service.dto.ForumTopCategoryDto;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.repository.PostRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ForumStatsService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public ForumStatsDto buildStats() {
        long totalPosts = postRepository.count();
        long totalComments = commentRepository.countCommentsTotal();
        long inactivePosts = postRepository.countByInactiveTrue();
        List<Object[]> rows = postRepository.countPostsGroupedByCategory(PageRequest.of(0, 10));
        List<ForumTopCategoryDto> top = new ArrayList<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 3) {
                continue;
            }
            top.add(ForumTopCategoryDto.builder()
                    .categoryId(toLong(row[0]))
                    .categoryName(row[1] != null ? row[1].toString() : "")
                    .postCount(toLong(row[2]))
                    .build());
        }
        return ForumStatsDto.builder()
                .totalPosts(totalPosts)
                .totalComments(totalComments)
                .inactivePosts(inactivePosts)
                .topCategories(top)
                .build();
    }

    /** Hibernate may return {@link Integer} or {@link Long} for JPQL aggregates depending on DB/driver. */
    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
