package project.repositories;

import org.springframework.data.repository.CrudRepository;
import project.models.GlobalSetting;
import project.models.enums.GlobalSettingsEnum;

import java.util.List;
import java.util.Optional;

public interface GlobalSettingsRepo extends CrudRepository<GlobalSetting, Integer> {

    List<GlobalSetting> findAll();

    Optional<GlobalSetting> findByCode(GlobalSettingsEnum code);
}
