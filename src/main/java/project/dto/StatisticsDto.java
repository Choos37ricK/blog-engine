package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatisticsDto {

    private Integer postsCount;

    private Integer likesCount;

    private Integer dislikesCount;

    private Integer viewsCount;

    private Long firstPublication;
}
