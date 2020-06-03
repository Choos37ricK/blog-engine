package project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InitInfoDto {

    private String title;

    private String subtitle;

    private String phone;

    private String email;

    private String copyright;

    private String copyrightFrom;
}
