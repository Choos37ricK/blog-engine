package project.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "post_comments")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false)
    private String text;

    public PostComment(Integer parentId, Integer postId, Integer userId, LocalDateTime time, String text) {
        this.parentId = parentId;
        this.postId = postId;
        this.userId = userId;
        this.time = time;
        this.text = text;
    }
}
