package project.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post_votes")
@NoArgsConstructor
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false)
    private Integer value;

    public PostVote(Integer userId, Integer postId, LocalDateTime time, Integer value) {
        this.userId = userId;
        this.postId = postId;
        this.time = time;
        this.value = value;
    }
}
