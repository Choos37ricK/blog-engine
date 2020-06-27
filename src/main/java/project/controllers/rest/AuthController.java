package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import project.dto.*;
import project.models.CaptchaCode;
import project.models.User;
import project.services.*;

import java.time.LocalDateTime;
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

    @Value("${response.host}")
    private String host;

    @Value("${server.port}")
    private String port;

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


        if (authService.checkAuthorization()) {
            User userFromDB = userService.findUserById(authService.getUserIdBySession());
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
        CaptchaCode captcha = captchaCodeService.findCaptchaByCodeAndSecretCode(
                registerDto.getCaptcha(), registerDto.getCaptchaSecret());

        Map<String, String> errors = authService.checkOnErrors(
                registerDto.getPassword(),
                captcha,
                userService.findUserByEmail(registerDto.getEmail()),
                new User(),
                registerDto.getName()
        );

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
                String.format("http://%s:%s/%s", host, port, "/src/main/resources/uploads/default-1.png")
        );
        userService.saveUser(newUser);

        /**
         * Добавлено удаление каптчи после использования
         */
        captchaCodeService.deleteCaptcha(captcha);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
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
        CaptchaCode captcha = captchaCodeService.findCaptchaByCodeAndSecretCode(
                passwordRestoreDto.getCaptcha(), passwordRestoreDto.getCaptchaSecret());

        Map<String, String> errors = authService.checkOnErrors(
                passwordRestoreDto.getPassword(),
                captcha,
                null,
                existUser,
                null
        );

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
            return ResponseEntity.ok(new ResultTrueFalseDto(false));
        }

        String token = RandomStringUtils.randomAlphanumeric(45).toLowerCase();

        user.setCode(token);
        userService.saveUser(user);

        String link = "http://localhost:8086/login/change-password/" + token;
        String message = "<a href=\"" + link + "\">Восстановить пароль</a>";
        emailService.send(email, "Восстановление пароля", message);

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