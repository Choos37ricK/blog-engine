package project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginDto {

    @JsonProperty("e_mail")
    private String email;

    private String password;
}
