package project.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import project.models.CaptchaCode;

import java.util.Optional;

public interface CaptchaCodesRepo extends CrudRepository<CaptchaCode, Integer> {
    @Transactional
    @Modifying
    @Query(value =
            "delete from captcha_codes " +
            "where  time_to_sec(timediff(now(), time)) > 3600", nativeQuery = true)
    void deleteOld();

    Optional<CaptchaCode> findByCodeAndSecretCode(String Code, String secretCode);
}
