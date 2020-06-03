package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
public class PostCommentDto {

    private Integer id;

    private LocalDateTime time;

    private String text;

    private CommentUserDto user;
}
