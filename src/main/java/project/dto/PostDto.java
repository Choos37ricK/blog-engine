package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {

    private Integer id;

    private String time;

    private PostUserDto user;

    private String title;

    private String announce;

    private Integer likeCount;

    private Integer dislikeCount;

    private Integer commentCount;

    private Integer viewCount;

}
