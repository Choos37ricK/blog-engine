package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public class PostListDto {

    private Integer count;

    private Collection<PostDto> posts;
}
