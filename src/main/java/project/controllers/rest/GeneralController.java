package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import project.controllers.exceptions.BadRequestException;
import project.controllers.exceptions.UnauthorizedException;
import project.dto.*;
import project.models.*;
import project.models.enums.ModerationStatusesEnum;
import project.services.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class GeneralController {

    private final AuthService authService;

    private final UserService userService;

    private final GlobalSettingsService globalSettingsService;

    private final TagService tagService;

    private final PostService postService;

    private final Post2TagService post2TagService;

    private final PostCommentService postCommentService;

    private final GeneralService generalService;

    @Value("${name.max.length}")
    private Integer nameMaxLength;

    @GetMapping("init")
    public ResponseEntity<InitInfoDto> initInfo() {
        InitInfoDto dto = new InitInfoDto(
                "DevPub",
                "Рассказы разработчиков",
                "+7 777 666-44-55",
                "ilyxa.girin@mail.ru",
                "Ilya Girin",
                "2019");
        return ResponseEntity.ok(dto);
    }

    @GetMapping("tag")
    public ResponseEntity<?> getTags(@RequestParam(required = false) String query) {
        List<Tag> tagList = query != null ? tagService.findByStartsWith(query) : tagService.findAllTags();

        Integer tagListSize = tagList.size();
        List<TagDto> tagDtoList = tagList.stream()
                .map(this::getTagDto)
                .sorted(Comparator.comparing(TagDto::getWeight).reversed())
                .collect(Collectors.toList());

        Float biggestWeight = tagDtoList.get(0).getWeight();
        tagDtoList.get(0).setWeight(1f);

        tagDtoList.forEach(tagDto -> {
            Float tagWeight = tagDto.getWeight();
            if (tagWeight < 0.5) {
                tagWeight *= (1/biggestWeight);
                tagDto.setWeight(tagWeight);
            }
        });

        return ResponseEntity.ok(new TagListDto(tagDtoList));
    }

    @GetMapping("settings")
    public ResponseEntity<?> getSettings() {
        return makeSettings(null);
    }

    @PutMapping("settings")
    public ResponseEntity<?> changeSettings(@RequestBody GlobalSettingsDto globalSettingsDto) {
        return makeSettings(globalSettingsDto);
    }

    @PostMapping("moderation")
    @ResponseStatus(HttpStatus.OK)
    public void moderation(@RequestBody ModerationPostDto moderationPostDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        Post moderatePost = postService.findPostById(moderationPostDto.getPostId());

        String decision = moderationPostDto.getDecision();
        if (decision.equals("accept")) {
            moderatePost.setModerationStatus(ModerationStatusesEnum.ACCEPTED);
        } else if (decision.equals("decline")) {
            moderatePost.setModerationStatus(ModerationStatusesEnum.DECLINED);
        }
        User moderator = userService.findUserById(authService.getUserIdBySession(sessionId));
        moderatePost.setModerator(moderator);

        postService.savePost(moderatePost);
    }

    @GetMapping("calendar")
    public ResponseEntity<?> calendar(@RequestParam(required = false) Integer year) {


//        if (year == null) {
//            year = LocalDate.now().getYear();
//        }

        return ResponseEntity.ok(postService.getCalendarDto(year));
    }

    @PostMapping("comment")
    public ResponseEntity<?> addComment(@RequestBody AddCommentDto addCommentDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        return checkOnErrorsComment(addCommentDto);
    }

    @SneakyThrows
    @PostMapping(value = "profile/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editProfileWithPhoto(@RequestParam(value = "photo", required = false) MultipartFile photo,
                                                  @ModelAttribute MyProfileDto myProfileDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        User exist = userService.findUserById(authService.getUserIdBySession(sessionId));

        ErrorsDto errorsDto = checkOnErrorsProfile(myProfileDto, exist, photo);
        if (errorsDto.getErrors().size() > 0) {
            return ResponseEntity.badRequest().body(errorsDto);
        }

        if (photo != null) {

            String type = photo.getContentType().split("/")[1];
            String randomName = RandomStringUtils.randomAlphanumeric(10);
            String basePath = "C:/Users/Norty/Desktop/Blog engine";
            String dir1 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
            String dir2 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
            String dir3 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
            String dstPath = String.format("/src/main/resources/uploads/%s/%s/%s/", dir1, dir2, dir3);
            File uploadFolder = new File(basePath + dstPath);

            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }
            File dstFile = new File(uploadFolder, randomName + "." + type);
            saveImage(50, photo, dstFile, type);

            String userPhoto = exist.getPhoto();

            exist.setPhoto(dstPath + randomName + "." + type);
        }

        exist.setName(myProfileDto.getName());
        exist.setEmail(myProfileDto.getEmail());

        if (myProfileDto.getPassword() != null) {
            exist.setPassword(myProfileDto.getPassword());
        }

        userService.saveUser(exist);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    @SneakyThrows
    @PostMapping("profile/my")
    public ResponseEntity<?> editProfile(@RequestBody MyProfileDto myProfileDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        User exist = userService.findUserById(authService.getUserIdBySession(sessionId));

        ErrorsDto errorsDto = checkOnErrorsProfile(myProfileDto, exist, null);
        if (errorsDto.getErrors().size() > 0) {
            return ResponseEntity.badRequest().body(errorsDto);
        }

        if (myProfileDto.getRemovePhoto() != null && myProfileDto.getRemovePhoto() == 1) { //разобраться с путями
            String basePath = "C:/Users/Norty/Desktop/Blog engine";
            String userPhoto = exist.getPhoto();

            String path = basePath + userPhoto;

            if (userPhoto != null) {
                new File(userPhoto.replace(
                        "http://localhost:8086", basePath)
                ).delete();
            }
            exist.setPhoto(null);
        }

        exist.setName(myProfileDto.getName());
        exist.setEmail(myProfileDto.getEmail());

        if (myProfileDto.getPassword() != null) {
            exist.setPassword(myProfileDto.getPassword());
        }

        userService.saveUser(exist);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    @SneakyThrows
    @PostMapping("image")
    public ResponseEntity<?> upload(@RequestPart("image") MultipartFile image) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        if (image.isEmpty()) {
            throw new BadRequestException();
        }

        String type = image.getContentType().split("/")[1];
        String randomName = RandomStringUtils.randomAlphanumeric(10);
        String dir1 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String dir2 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String dir3 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String basePath = "C:/Users/Norty/Desktop/Blog engine";
        String dstPath = String.format("/src/main/resources/uploads/%s/%s/%s/", dir1, dir2, dir3);
        File uploadFolder = new File(basePath + dstPath);

        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }
        File dstFile = new File(uploadFolder, randomName + "." + type);
        saveImage(400, image, dstFile, type);

        return ResponseEntity.ok(dstPath + randomName + "." + type);
    }

    @SneakyThrows
    private void saveImage(Integer newWidth, MultipartFile image, File dstFile, String type) {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image.getBytes()));
        int newHeight = (int) Math.round(bufferedImage.getHeight() / (bufferedImage.getWidth() / (double) newWidth));
        BufferedImage newImage = Scalr.resize(bufferedImage, Scalr.Method.ULTRA_QUALITY, newWidth, newHeight);

        ImageIO.write(newImage, type, dstFile);
    }

    private ErrorsDto checkOnErrorsProfile(MyProfileDto myProfileDto, User user, MultipartFile photo) {
        HashMap<String, String> errors = new HashMap<>();

        String newEmail = myProfileDto.getEmail();

        if (!newEmail.equals(user.getEmail())) {
            User exist = userService.findUserByEmail(newEmail);

            if (exist != null) {
                errors.put("email", "Этот e-mail уже зарегистрирован");
            }
        }

        if (photo != null && photo.getSize() > 5242880) {
            errors.put("photo", "Фото слишком большое, нужно не более 5 Мб");
        }

        if (!myProfileDto.getName().matches("^[A-Za-zА-Яа-яЁё]{1," + nameMaxLength + "}$")) {
            errors.put("name", "Имя указано неверно или имеет недопустимую длину (1-30)");
        }

        if (myProfileDto.getPassword() != null && myProfileDto.getPassword().length() < 6) {
            errors.put("password", "Пароль короче 6-ти символов");
        }

        return new ErrorsDto(false, errors);
    }

    private ResponseEntity<?> checkOnErrorsComment(AddCommentDto addCommentDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        Integer parentId = addCommentDto.getParentId();
        Integer postId = addCommentDto.getPostId();
        String text = addCommentDto.getText();

        if(parentId != null && postCommentService.findById(parentId) == null
                || postId == null || postService.findPostById(postId) == null
        ) {
            throw new BadRequestException();
        }

        HashMap<String, String> errors = new HashMap<>();

        if (text.isEmpty()) {
            errors.put("text", "Текст комментария не задан");
        } else if (text.length() < 10) {
            errors.put("text", "Текст комментария слишком короткий");
        }

        if (errors.size() > 0) {
            return ResponseEntity.badRequest().body(new ErrorsDto(false, errors));
        }

        return ResponseEntity.ok(
                new AddedCommentIdDto(
                        postCommentService.saveComment(addCommentDto, authService.getUserIdBySession(sessionId))
                )
        );
    }

    private TagDto getTagDto(Tag tag) {
        Integer postWithTagCount = post2TagService.countPostsWithTag(tag.getId());
        Integer postTotalCount = postService.countPosts();
        Float weight = (float)postWithTagCount / postTotalCount;

        return new TagDto(tag.getName(), weight);
    }

    /**
     * Метод сохранения и/или возврата настроек (убирает дублирование кода)
     */
    private ResponseEntity<?> makeSettings(GlobalSettingsDto globalSettingsDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (authService.checkAuthorization(sessionId)) {
            User userFromDB = userService.findUserById(authService.getUserIdBySession(sessionId));
            if (userFromDB.getIsModerator() == 1) {
                List<GlobalSetting> settings = globalSettingsService.findAll();

                if (globalSettingsDto != null) {
                    settings.get(0).setValue(globalSettingsDto.getMultiuserMode() ? "YES" : "NO");
                    settings.get(1).setValue(globalSettingsDto.getPostPremoderation() ? "YES" : "NO");
                    settings.get(2).setValue(globalSettingsDto.getStatisticsIsPublic() ? "YES" : "NO");

                    globalSettingsService.saveSettings(settings);
                } else {
                    globalSettingsDto = new GlobalSettingsDto(
                            settings.get(0).getValue().equals("YES"),
                            settings.get(1).getValue().equals("YES"),
                            settings.get(2).getValue().equals("YES")
                    );
                }

                return ResponseEntity.ok(globalSettingsDto);
            }
        }
        return ResponseEntity.ok(new ResultTrueFalseDto(false));
    }
}
