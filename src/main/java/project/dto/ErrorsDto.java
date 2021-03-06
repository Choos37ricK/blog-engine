package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorsDto {

    private Boolean result;

    private Map<String, String> errors;
}
