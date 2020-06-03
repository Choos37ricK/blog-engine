package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.models.GlobalSetting;
import project.models.enums.GlobalSettingsEnum;
import project.repositories.GlobalSettingsRepo;

import java.util.List;

@Service
@AllArgsConstructor
public class GlobalSettingsService {

    private final GlobalSettingsRepo globalSettingsRepo;

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
