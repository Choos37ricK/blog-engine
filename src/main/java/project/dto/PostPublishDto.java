package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PostPublishDto {

    private Long timestamp;

    private Byte active;

    private String title;

    private String text;

    private List<String> tags;
}
