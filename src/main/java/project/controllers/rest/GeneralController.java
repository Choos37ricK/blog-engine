package project.controllers.rest;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import project.controllers.exceptions.BadRequestException;
import project.controllers.exceptions.NotFoundException;
import project.controllers.exceptions.UnauthorizedException;
import project.dto.*;
import project.models.*;
import project.models.enums.ModerationStatusesEnum;
import project.services.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/")
@AllArgsConstructor
public class GeneralController {

    private final AuthService authService;

    private final UserService userService;

    private final GlobalSettingsService globalSettingsService;

    private final TagService tagService;

    private final PostService postService;

    private final Post2TagService post2TagService;

    private final PostCommentService postCommentService;

    private final GeneralService generalService;

    private final ImagePath imagePath;

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

        return checkOnErrors(addCommentDto);
    }

    @PostMapping("profile/my")
    public ResponseEntity<?> myProfile(@RequestBody MyProfileDto myProfileDto) {
        return null;
    }

    @PostMapping("image")
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile multipartFile) throws IOException {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        if (multipartFile.isEmpty())
            throw new BadRequestException();

        User user = userService.findUserById(authService.getUserIdBySession(sessionId));

        String photo = user.getPhoto();
        if (photo.equals(imagePath.getDefaultImagePath())) {
            Integer id = generalService.saveImage(multipartFile.getBytes(), multipartFile.getContentType());
            String URL = imagePath.getImagePath()  + id;
            userService.updatePhoto(user, URL);
            return ResponseEntity.ok(URL);
        } else {
            Integer oldId = Integer.valueOf(photo.replace(imagePath.getImagePath(), ""));

            try {
                generalService.updateImage(multipartFile.getBytes(), multipartFile.getContentType(), oldId);
                return ResponseEntity.ok(imagePath.getImagePath()  + oldId);
            } catch(NotFoundException e) {
                throw new BadRequestException();
            }
        }
    }

    private ResponseEntity<?> checkOnErrors(AddCommentDto addCommentDto) {
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
