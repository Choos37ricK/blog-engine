package project.services;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import project.dto.CaptchaDto;
import project.models.CaptchaCode;
import project.repositories.CaptchaCodesRepo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@Service
@AllArgsConstructor
public class AuthService {

    private final Map<String, Integer> authorizedUsers;

    public Integer getUserIdBySession(String sessionId) {

        return authorizedUsers.get(sessionId);
    }

    public Boolean checkAuthorization(String sessionId) {
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
