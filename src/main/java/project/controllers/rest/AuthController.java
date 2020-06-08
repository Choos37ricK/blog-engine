package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import project.dto.*;
import project.models.CaptchaCode;
import project.models.User;
import project.services.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final PostService postService;

    private final AuthService authService;

    private final CaptchaCodeService captchaCodeService;

    private final EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${name.max.length}")
    private Integer nameMaxLength;

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {

        User userFromDB = userService.findUserByEmail(loginDto.getEmail());

        if (userFromDB != null && passwordEncoder.matches(loginDto.getPassword(), userFromDB.getPassword())) {
            authService.saveSession(userFromDB.getId());
            return getAuthUserResponseEntityDto(userFromDB);
        }
        return ResponseEntity.ok(new ResultTrueFalseDto(false));
    }

    @GetMapping("check")
    public ResponseEntity<?> check() {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();

        if (authService.checkAuthorization(sessionId)) {
            User userFromDB = userService.findUserById(authService.getUserIdBySession(sessionId));
            return getAuthUserResponseEntityDto(userFromDB);
        }
        return ResponseEntity.ok(new ResultTrueFalseDto(false));
    }

    @GetMapping("logout")
    public ResponseEntity<ResultTrueFalseDto> logout() {

        authService.logout();

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody RegisterDto registerDto) {
        return checkOnErrors(registerDto);
    }

    @GetMapping("captcha")
    public ResponseEntity<?> getCaptcha() {
        return ResponseEntity.ok(captchaCodeService.getCaptchaDto());
    }

    @PostMapping("restore")
    public ResponseEntity<?> restore(@RequestBody RestoreDto restoreDto) {
        return recoverPassword(restoreDto.getEmail());
    }

    @PostMapping("password")
    public ResponseEntity<?> password(@RequestBody PasswordRestoreDto passwordRestoreDto) {
        User existUser = userService.findUserByRecoverCode(passwordRestoreDto.getCode());

        Map<String, String> errors = new HashMap<>();

        if (existUser == null) {
            errors.put("code", "Ссылка для восстановления пароля устарела.\n" +
                    "<a href=/login/restore-password>Запросить ссылку снова</a>");
        }

        if (passwordRestoreDto.getPassword().length() < 6) {
            errors.put("password", "Пароль короче 6-ти символов");
        }

        CaptchaCode captcha = captchaCodeService.findCaptchaByCodeAndSecretCode(
                passwordRestoreDto.getCaptcha(), passwordRestoreDto.getCaptchaSecret());
        if (captcha == null) {
            errors.put("captcha", "Код с картинки введён неверно");
        }

        if (errors.size() > 0) {
            return ResponseEntity.ok(new ErrorsDto(false, errors));
        }

        existUser.setCode("");
        existUser.setPassword(passwordEncoder.encode(passwordRestoreDto.getPassword()));
        userService.saveUser(existUser);

        /**
         * Добавлено удаление каптчи после использования
         */
        captchaCodeService.deleteCaptcha(captcha);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    private ResponseEntity<?> recoverPassword(String email) {
        User user = userService.findUserByEmail(email);

        if (user == null) {
            return ResponseEntity.badRequest().body(new ResultTrueFalseDto(false));
        }

        String token = RandomStringUtils.randomAlphanumeric(45).toLowerCase();

        user.setCode(token);
        userService.saveUser(user);

        String link = "http://localhost:8086/login/change-password/" + token;
        String message = String.format("Для восстановления пароля перейдите по ссылке %s", link );
        emailService.send(email, "Восстановление пароля", message);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    private ResponseEntity<?> checkOnErrors(RegisterDto registerDto) {
        Map<String, String> errors = new HashMap<>();

        User existUser = userService.findUserByEmail(registerDto.getEmail());
        if (existUser != null) {
            errors.put("email", "Этот e-mail уже зарегистрирован");
        }

        if (!registerDto.getName().matches("^[A-Za-zА-Яа-яЁё]{1," + nameMaxLength + "}$")) {
            errors.put("name", "Имя указано неверно или имеет недопустимую длину (1-30)");
        }

        if (registerDto.getPassword().length() < 6) {
            errors.put("password", "Пароль короче 6-ти символов");
        }

        CaptchaCode captcha = captchaCodeService.findCaptchaByCodeAndSecretCode(
                registerDto.getCaptcha(), registerDto.getCaptchaSecret());
        if (captcha == null) {
            errors.put("captcha", "Код с картинки введён неверно");
        }

        if (errors.size() > 0) {
            return ResponseEntity.ok(new ErrorsDto(false, errors));
        }

        User newUser = new User(
                (byte) 0,
                LocalDateTime.now(),
                registerDto.getName(),
                registerDto.getEmail(),
                passwordEncoder.encode(registerDto.getPassword()),
                null,
                "http://localhost:8086/src/main/resources/uploads/default-1.png"
        );
        userService.saveUser(newUser);

        /**
         * Добавлено удаление каптчи после использования
         */
        captchaCodeService.deleteCaptcha(captcha);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    private ResponseEntity<?> getAuthUserResponseEntityDto(User userFromDB) {
        Integer moderationCount = null;

        if (userFromDB.getIsModerator() == 1) {
            moderationCount = postService.countPostsNeedModeration();
            return getAuthUserDto(userFromDB, true, true, moderationCount);
        }
        return getAuthUserDto(userFromDB, false, false, moderationCount);
    }

    private ResponseEntity<AuthUserDto> getAuthUserDto
            (User userFromDB, Boolean isModerator, Boolean settings, Integer moderationCount) {

        AuthUserInfoDto userInfoDto = new AuthUserInfoDto(
                userFromDB.getId(),
                userFromDB.getName(),
                userFromDB.getPhoto(),
                userFromDB.getEmail(),
                isModerator,
                moderationCount,
                settings);

        return ResponseEntity.ok(new AuthUserDto(true, userInfoDto));
    }
}