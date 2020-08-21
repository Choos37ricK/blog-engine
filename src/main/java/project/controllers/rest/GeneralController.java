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

    @Value("${upload.path}")
    private String location;

    @Value("${name.max.length}")
    private Integer nameMaxLength;

    @Value("${password.min.length}")
    private Integer passwordMinLength;

    @Value("${text.min.length}")
    private Integer textMinLength;

    @Value("${max.photo.size}")
    private Integer maxPhotoSize;

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

        if (tagList.size() == 0) {
            return ResponseEntity.ok(null);
        }

        List<TagDto> tagDtoList = tagList.stream()
                .map(this::getTagDto)
                .sorted(Comparator.comparing(TagDto::getWeight).reversed())
                .collect(Collectors.toList());

        tagDtoList = tagService.setWeights(tagDtoList);

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

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        Post moderatePost = postService.findPostById(moderationPostDto.getPostId());

        String decision = moderationPostDto.getDecision();
        if (decision.equals("accept")) {
            moderatePost.setModerationStatus(ModerationStatusesEnum.ACCEPTED);
        } else if (decision.equals("decline")) {
            moderatePost.setModerationStatus(ModerationStatusesEnum.DECLINED);
        }
        User moderator = userService.findUserById(authService.getUserIdBySession());
        moderatePost.setModerator(moderator);

        postService.savePost(moderatePost);
    }

    @GetMapping("calendar")
    public ResponseEntity<?> calendar(@RequestParam(required = false) Integer year) {

        return ResponseEntity.ok(postService.getCalendarDto(year));
    }

    @PostMapping("comment")
    public ResponseEntity<?> addComment(@RequestBody AddCommentDto addCommentDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        return checkOnErrorsComment(addCommentDto);
    }

    @SneakyThrows
    @PostMapping("profile/my")
    public ResponseEntity<?> editProfile(@RequestBody MyProfileDto myProfileDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        User exist = userService.findUserById(authService.getUserIdBySession());

        ErrorsDto errorsDto = checkOnErrorsProfile(myProfileDto, exist, null);
        if (errorsDto.getErrors().size() > 0) {
            return ResponseEntity.badRequest().body(errorsDto);
        }

        if (myProfileDto.getRemovePhoto() != null && myProfileDto.getRemovePhoto() == 1) {
            String userPhoto = exist.getPhoto();

            if (userPhoto != null) {
                new File(userPhoto.replaceFirst("/", "")).delete();
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

    private String prepareAndSaveImage(MultipartFile image) {
        String type = image.getContentType().split("/")[1];
        String randomName = RandomStringUtils.randomAlphanumeric(10);
        String dir1 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String dir2 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String dir3 = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String dstPath = String.format("%s%s/%s/%s/", location, dir1, dir2, dir3);
        File uploadFolder = new File(dstPath);

        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }
        File dstFile = new File(uploadFolder, randomName + "." + type);
        saveImage(50, image, dstFile, type);

        return String.format("/%s%s.%s", dstPath, randomName, type);
    }

    @SneakyThrows
    @PostMapping(value = "profile/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editProfileWithPhoto(@RequestParam(value = "photo") MultipartFile photo,
                                                  @ModelAttribute MyProfileDto myProfileDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        User exist = userService.findUserById(authService.getUserIdBySession());

        ErrorsDto errorsDto = checkOnErrorsProfile(myProfileDto, exist, photo);
        if (errorsDto.getErrors().size() > 0) {
            return ResponseEntity.badRequest().body(errorsDto);
        }

        if (photo != null) {
            String imagePath = prepareAndSaveImage(photo);
            String oldUserPhoto = exist.getPhoto();

            if (oldUserPhoto != null) {
                new File(oldUserPhoto.replaceFirst("/", "")).delete();
            }
            exist.setPhoto(imagePath);
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

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        String imagePath = prepareAndSaveImage(image);

        return ResponseEntity.ok(imagePath);
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

        if (photo != null && photo.getSize() > maxPhotoSize) {
            errors.put("photo", "Фото слишком большое, нужно не более 5 Мб");
        }

        if (!myProfileDto.getName().matches("^[A-Za-zА-Яа-яЁё]{1," + nameMaxLength + "}$")) {
            errors.put("name", "Имя указано неверно или имеет недопустимую длину (1-30)");
        }

        if (myProfileDto.getPassword() != null && myProfileDto.getPassword().length() < passwordMinLength) {
            errors.put("password", "Пароль короче 6-ти символов");
        }

        return new ErrorsDto(false, errors);
    }

    private ResponseEntity<?> checkOnErrorsComment(AddCommentDto addCommentDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        Integer parentId = addCommentDto.getParentId();
        Integer postId = addCommentDto.getPostId();
        String text = addCommentDto.getText();

        if(parentId != null && postCommentService.findById(parentId) == null
                || postId == null || postService.findPostById(postId) == null
        ) {
            throw new BadRequestException("Комментарий или пост, на который вы хотите ответить не существует!");
        }

        HashMap<String, String> errors = new HashMap<>();

        if (text.isEmpty() || text.length() < textMinLength) {
            errors.put("text", "Текст комментария не задан или слишком короткий");
        }

        if (errors.size() > 0) {
            return ResponseEntity.badRequest().body(new ErrorsDto(false, errors));
        }

        return ResponseEntity.ok(
                new AddedCommentIdDto(
                        postCommentService.saveComment(addCommentDto, authService.getUserIdBySession())
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
    @SneakyThrows
    private ResponseEntity<?> makeSettings(GlobalSettingsDto globalSettingsDto) {

        List<GlobalSetting> settings = globalSettingsService.findAll();
        if (globalSettingsDto != null) {
            if (authService.checkAuthorization()) {
                User userFromDB = userService.findUserById(authService.getUserIdBySession());
                if (userFromDB.getIsModerator() == 1) {
                    settings.get(0).setValue(globalSettingsDto.getMultiuserMode() ? "YES" : "NO");
                    settings.get(1).setValue(globalSettingsDto.getPostPremoderation() ? "YES" : "NO");
                    settings.get(2).setValue(globalSettingsDto.getStatisticsIsPublic() ? "YES" : "NO");

                    globalSettingsService.saveSettings(settings);
                    return ResponseEntity.ok(globalSettingsDto);
                }
            }
            throw new BadRequestException("У вас нет прав для совершения данного действия");
        } else {
            globalSettingsDto = new GlobalSettingsDto(
                    settings.get(0).getValue().equals("YES"),
                    settings.get(1).getValue().equals("YES"),
                    settings.get(2).getValue().equals("YES")
            );
            return ResponseEntity.ok(globalSettingsDto);
        }
    }
}
