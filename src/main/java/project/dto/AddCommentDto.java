package project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddCommentDto {

    @JsonProperty("parent_id")
    private Integer parentId;

    @JsonProperty("post_id")
    private Integer postId;

    private String text;
}
