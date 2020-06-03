package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserDto {

    private Boolean result;

    private AuthUserInfoDto user;
}
