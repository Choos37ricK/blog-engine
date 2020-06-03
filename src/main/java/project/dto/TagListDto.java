package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TagListDto {
    private List<TagDto> tags;
}
