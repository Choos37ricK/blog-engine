package project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GlobalSettingsDto {

    @JsonProperty("MULTIUSER_MODE")
    private Boolean multiuserMode;

    @JsonProperty("POST_PREMODERATION")
    private Boolean postPremoderation;

    @JsonProperty("STATISTICS_IS_PUBLIC")
    private Boolean statisticsIsPublic;
}
