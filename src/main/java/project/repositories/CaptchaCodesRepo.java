package project.repositories;

import org.springframework.data.repository.CrudRepository;
import project.models.CaptchaCode;

public interface CaptchaCodesRepo extends CrudRepository<CaptchaCode, Integer> {
}
