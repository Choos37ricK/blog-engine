package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostPublishDto {

    private Long timestamp;

    private Byte active;

    private String title;

    private String text;

    private String[] tags;
}
