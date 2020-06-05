package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import project.dto.*;
import project.models.CaptchaCode;
import project.models.ImagePath;
import project.models.User;
import project.services.AuthService;
import project.services.CaptchaCodeService;
import project.services.PostService;
import project.services.UserService;

import java.time.LocalDate;
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

        CaptchaCode captcha = captchaCodeService.findCaptchaByCode(registerDto.getCaptcha());
        if (captcha == null) {
            errors.put("captcha", "Код с картинки введён неверно");
        }

        if (errors.size() > 0) {
            return ResponseEntity.badRequest().body(new ErrorsDto(false, errors));
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