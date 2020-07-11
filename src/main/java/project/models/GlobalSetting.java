package project.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import project.models.enums.GlobalSettingsEnum;

import javax.persistence.*;

@Data
@Entity
@Table(name = "global_settings")
@NoArgsConstructor
public class GlobalSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GlobalSettingsEnum code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String value;

    public GlobalSetting(GlobalSettingsEnum code, String name, String value) {
        this.code = code;
        this.name = name;
        this.value = value;
    }
}
