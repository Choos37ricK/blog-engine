package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.controllers.exceptions.BadRequestException;
import project.controllers.exceptions.UnauthorizedException;
import project.dto.*;
import project.models.GlobalSetting;
import project.models.Post;
import project.models.PostComment;
import project.models.User;
import project.models.enums.GlobalSettingsEnum;
import project.services.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    private final PostCommentService postCommentService;

    private final PostVoteService postVoteService;

    private final UserService userService;

    private final TagService tagService;

    private final AuthService authService;

    private final Post2TagService post2TagService;

    private final GlobalSettingsService globalSettingsService;

    @Value("${title.min.length}")
    private Integer titleMinLength;

    @Value("${text.min.length}")
    private Integer textMinLength;

    @Value("${announce.length}")
    private Integer announceLength;

    @GetMapping
    public ResponseEntity<?> getPosts(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String mode
    ) {
        List<Post> postList = postService.getPostsBySort(mode, offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(postService.countPosts(), dtos));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String query
    ) {
        List<Post> postList = postService.findPostsByQuery(query, offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(postService.countPostsByQuery(query), dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Integer id) {
        User user = userService.findUserById(authService.getUserIdBySession());

        Post post = postService.getPostByIdAndModerationStatus(id, user);

        if (post == null) {
            throw new BadRequestException("Данный пост вам не доступен");
        }

        return ResponseEntity.ok(getOnePostDto(post));
    }

    @GetMapping("/byDate")
    public ResponseEntity<PostListDto> findPostsByDate(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String date
    ) {
        List<Post> postList = postService.getPostsByDate(date, offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(postService.countPostsByDate(date), dtos));
    }

    @GetMapping("/byTag")
    public ResponseEntity<?> findPostsByTag(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String tag
    ) {
        List<Post> postList = postService.findPostsByTag(tag, offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(postService.countPostsByTag(tag), dtos));
    }

    @GetMapping("/moderation")
    public ResponseEntity<?> getPostsByNeedModeration(@RequestParam Integer offset,
                                                      @RequestParam Integer limit,
                                                      @RequestParam String status
    ) {
        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        List<Post> postList = postService.getPostsByNeedModeration(
                status,
                userService.findUserById(authService.getUserIdBySession()),
                offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(postService.countPostsByNeedModeration(
                status,
                userService.findUserById(authService.getUserIdBySession())),
                dtos
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(@RequestParam Integer offset,
                                        @RequestParam Integer limit,
                                        @RequestParam String status
    ) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        List<Post> postList = postService.getMyPostsByStatus(authService.getUserIdBySession(), status, offset, limit);

        List<PostDto> dtos = getPostDtoList(postList);

        return ResponseEntity.ok(new PostListDto(
                postService.countMyPostsByStatus(authService.getUserIdBySession(), status),
                dtos
        ));
    }

    @PostMapping
    public ResponseEntity<?> publishPost(@RequestBody PostPublishDto postPublishDto) {
        return checkOnErrors(postPublishDto, null, "add");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editPost(@PathVariable Integer id, @RequestBody PostPublishDto postPublishDto) {
        return checkOnErrors(postPublishDto, id, "edit");
    }

    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestBody VotePostIdDto votePostIdDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        ResultTrueFalseDto isLiked = postVoteService.votePost(
                votePostIdDto.getPostId(), authService.getUserIdBySession(), 1);
        return isLiked.getResult() ? ResponseEntity.ok(isLiked) : ResponseEntity.badRequest().body(isLiked);
    }

    @PostMapping("/dislike")
    public ResponseEntity<?> dislike(@RequestBody VotePostIdDto votePostIdDto) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        ResultTrueFalseDto isDisliked = postVoteService.votePost(
                votePostIdDto.getPostId(), authService.getUserIdBySession(), -1);
        return isDisliked.getResult() ? ResponseEntity.ok(isDisliked) : ResponseEntity.badRequest().body(isDisliked);
    }

    private ResponseEntity<?> checkOnErrors(PostPublishDto postPublishDto, Integer postId, String type) {

        if (!authService.checkAuthorization()) {
            throw new UnauthorizedException();
        }

        String title = postPublishDto.getTitle();
        String text = postPublishDto.getText();

        HashMap<String, String> errors = new HashMap<>();

        if (title.isEmpty()) {
            errors.put("title", "Заголовок не установлен");
        } else if (title.length() < titleMinLength) {
            errors.put("title", "Заголовок публикации слишком короткий");
        }

        if (text.isEmpty()) {
            errors.put("text", "Текст публикации не установлен");
        } else if (text.length() < textMinLength) {
            errors.put("text", "Текст публикации слишком короткий");
        }

        if (errors.size() > 0) {
            return ResponseEntity.badRequest().body(new ErrorsDto(false, errors));
        }

        return savePostAndTags(postPublishDto, postId, type);
    }

    @SneakyThrows
    private ResponseEntity<?> savePostAndTags(PostPublishDto postPublishDto, Integer id, String type) {
        User author = userService.findUserById(authService.getUserIdBySession());

        GlobalSetting globalSetting = globalSettingsService.getGlobalSettingByCode(GlobalSettingsEnum.MULTIUSER_MODE);

        if (globalSetting.getValue().equals("NO") && author.getIsModerator() == 0) {
            throw new BadRequestException("На данный момент публикация постов запрещена!");
        }

        globalSetting = globalSettingsService.getGlobalSettingByCode(GlobalSettingsEnum.POST_PREMODERATION);

        boolean premoderation = true;
        if (globalSetting.getValue().equals("NO")) {
            premoderation = false;
        }

        Integer postId = type.equals("add") ?
                postService.addPost(postPublishDto, author, premoderation)
                : postService.editPost(postPublishDto, author, id, premoderation);

        List<Integer> tagIds = tagService.saveTags(postPublishDto.getTags());

        post2TagService.savePost2Tag(postId, tagIds);

        return ResponseEntity.ok(new ResultTrueFalseDto(true));
    }

    private PostDto getPostDto(Post post) {

        PostUserDto authorDto = new PostUserDto(post.getAuthor().getId(), post.getAuthor().getName());

        Integer postId = post.getId();
        Integer likeCount = postVoteService.countVotesByPostIdAndValue(postId, 1);
        Integer dislikeCount = postVoteService.countVotesByPostIdAndValue(postId, -1);
        Integer commentCount = postCommentService.countByPostId(postId);

        return new PostDto(
                postId,
                post.getTime(),
                authorDto,
                post.getTitle(),
                post.getText()
                        .replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "")
                        .substring(0, announceLength),
                likeCount,
                dislikeCount,
                commentCount,
                post.getViewCount()
        );
    }

    private List<PostDto> getPostDtoList(List<Post> postList) {
        return postList
                .stream()
                .map(this::getPostDto)
                .collect(toList());
    }

    private OnePostDto getOnePostDto(Post post) {
        PostDto postDto = getPostDto(post);

        List<PostComment> commentList = postCommentService.findAllByPostId(postDto.getId());
        List<PostCommentDto> comments = commentList.stream().map(this::getPostCommentDto).collect(Collectors.toList());

        List<String> tags = tagService.findAllByPostId(postDto.getId());

        return new OnePostDto(
                postDto.getId(),
                postDto.getTime(),
                postDto.getUser(),
                postDto.getTitle(),
                postDto.getAnnounce(),
                postDto.getLikeCount(),
                postDto.getDislikeCount(),
                postDto.getCommentCount(),
                postDto.getViewCount(),
                post.getText(),
                comments,
                tags
        );
    }

    private PostCommentDto getPostCommentDto(PostComment comment) {
        User commentUser = userService.findUserById(comment.getUserId());

        return new PostCommentDto(
                comment.getId(),
                comment.getTime(),
                comment.getText(),
                new CommentUserDto(
                        commentUser.getId(),
                        commentUser.getName(),
                        commentUser.getPhoto()
                )
        );
    }
}
