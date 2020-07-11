package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.models.GlobalSetting;
import project.models.enums.GlobalSettingsEnum;
import project.repositories.GlobalSettingsRepo;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@AllArgsConstructor
public class GlobalSettingsService {

    private final GlobalSettingsRepo globalSettingsRepo;

    @PostConstruct
    public void init() {
        List<GlobalSetting> exist = globalSettingsRepo.findAll();

        if (exist.size() == 0) {
            exist.add(new GlobalSetting(
                    GlobalSettingsEnum.MULTIUSER_MODE,
                    "Многопользовательский режим",
                    "YES"
            ));
            exist.add(new GlobalSetting(
                    GlobalSettingsEnum.POST_PREMODERATION,
                    "Премодерация постов",
                    "YES"
            ));
            exist.add(new GlobalSetting(
                    GlobalSettingsEnum.STATISTICS_IS_PUBLIC,
                    "Показывать всем статистику блога",
                    "YES"
            ));

            saveSettings(exist);
        }
    }

    public List<GlobalSetting> findAll() {
        return globalSettingsRepo.findAll();
    }

    public void saveSettings(List<GlobalSetting> settings) {
        settings.forEach(globalSettingsRepo::save);
    }

    public GlobalSetting getGlobalSettingByCode(GlobalSettingsEnum code) {
        return globalSettingsRepo.findByCode(code).orElse(null);
    }
}
