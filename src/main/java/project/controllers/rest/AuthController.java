package project.controllers.rest;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import project.controllers.exceptions.BadRequest;
import project.controllers.exceptions.NotFoundException;
import project.dto.*;
import project.models.User;
import project.services.AuthService;
import project.services.PostService;
import project.services.UserService;


@RestController
@RequestMapping("/api/auth/")
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    private final PostService postService;

    private final AuthService authService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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