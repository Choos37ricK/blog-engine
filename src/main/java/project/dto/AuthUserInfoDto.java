package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUserInfoDto {

    private Integer id;

    private String name;

    private String photo;

    private String email;

    private Boolean moderation;

    private Integer moderationCount;

    private Boolean settings;
}
