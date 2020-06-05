package project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterDto {

    @JsonProperty("e_mail")
    private String email;

    private String name;

    private String password;

    private String captcha;

    @JsonProperty("captcha_secret")
    private String captchaSecret;
}
