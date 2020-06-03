package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class PostPublishErrorsDto {

    private Boolean result;

    private Map<String, String> errors;
}
