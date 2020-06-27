package project.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import project.models.CaptchaCode;
import project.models.User;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Map<String, Integer> authorizedUsers;

    @Value("${name.max.length}")
    private Integer nameMaxLength;

    @Value("${password.min.length}")
    private Integer passwordMinLength;

    public Map<String, String> checkOnErrors(
            String password,
            CaptchaCode captcha,
            User userByEmail,
            User userByRecoverCode,
            String name
    ) {
        Map<String, String> errors = new HashMap<>();

        if (userByEmail != null) {
            errors.put("email", "Этот e-mail уже зарегистрирован");
        }

        if (name != null && !name.matches("^[A-Za-zА-Яа-яЁё]{1," + nameMaxLength + "}$")) {
            errors.put("name", "Имя указано неверно или имеет недопустимую длину (1-30)");
        }

        if (password.length() < passwordMinLength) {
            errors.put("password", "Пароль короче 6-ти символов");
        }

        if (captcha == null) {
            errors.put("captcha", "Код с картинки введён неверно");
        }

        if (userByRecoverCode == null) {
            errors.put("code", "Ссылка для восстановления пароля устарела.\n" +
                    "<a href=/login/restore-password>Запросить ссылку снова</a>");
        }

        return errors;
    }

    public Integer getUserIdBySession() {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        return authorizedUsers.get(sessionId);
    }

    public Boolean checkAuthorization() {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        Integer userId = authorizedUsers.get(sessionId);
        return userId != null;
    }

    public void saveSession(Integer userId) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        authorizedUsers.put(sessionId, userId);
    }

    public void logout() {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        authorizedUsers.remove(sessionId);
    }


}
