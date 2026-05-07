package tn.esprit.forums_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "post_media", indexes = @Index(name = "idx_post_media_post", columnList = "post_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Post post;

    /** Raw image bytes; LONGBLOB avoids Hibernate 6 mapping {@code byte[]} {@code @Lob} to LONGTEXT on MySQL. */
    @JdbcTypeCode(SqlTypes.BLOB)
    @Column(name = "image_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(nullable = false, length = 120)
    private String contentType;

    @Column(nullable = false)
    private int sortOrder;
}
