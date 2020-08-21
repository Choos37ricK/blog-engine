package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostCommentDto {

    private Integer id;

    private Long timestamp;

    private String text;

    private CommentUserDto user;
}
