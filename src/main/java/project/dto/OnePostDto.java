package project.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OnePostDto extends PostDto {

    private String text;

    private List<PostCommentDto> comments;

    private List<String> tags;

    public OnePostDto(Integer id, Long timestamp, PostUserDto user, String title, String announce, Integer likeCount,
                      Integer dislikeCount, Integer commentCount, Integer viewCount, String text, List<PostCommentDto> comments,
                      List<String> tags) {
        super(id, timestamp, user, title, announce, likeCount, dislikeCount, commentCount, viewCount);
        this.text = text;
        this.comments = comments;
        this.tags = tags;
    }
}
