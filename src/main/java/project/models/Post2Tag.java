package project.models;

import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Table(name = "post2tag")
public class Post2Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "tag_id")
    private Integer tagId;

    public Post2Tag(Integer postId, Integer tagId) {
        this.postId = postId;
        this.tagId = tagId;
    }
}
