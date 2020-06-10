package project.controllers.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import project.controllers.exceptions.UnauthorizedException;
import project.dto.*;
import project.models.Post;
import project.models.PostComment;
import project.models.User;
import project.models.enums.ModerationStatusesEnum;
import project.services.*;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
        List<Post> postList = postService.getPostsBySort(mode);

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        if (mode.equals("popular")) {
            dtos.sort(Comparator.comparing(PostDto::getCommentCount).reversed());
        } else if (mode.equals("best")) {
            dtos.sort(Comparator.comparing(PostDto::getLikeCount).reversed());
        }

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPosts(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String query
    ) {
        List<Post> postList = postService.findPostsByQuery(query);

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Integer id) {
        Post post;
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        User user = userService.findUserById(authService.getUserIdBySession(sessionId));
        if (authService.checkAuthorization(sessionId)) {
            Byte isModerator = user.getIsModerator();
            post = postService.getPostByIdAndModerationStatus(id, isModerator);
        } else {
            post = postService.getPostByIdAndModerationStatus(id, (byte) 0);
            if (post != null) {
                if (!post.getAuthor().getId().equals(user.getId())) {
                    if (!(post.getIsActive() == (byte) 1 && post.getModerationStatus() == ModerationStatusesEnum.ACCEPTED)) {
                        return ResponseEntity.badRequest().body(new ResultTrueFalseDto(false));
                    }
                }
            }
        }

        return ResponseEntity.ok(getOnePostDto(post));
    }

    @GetMapping("/byDate")
    public ResponseEntity<PostListDto> findPostsByDate(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String date
    ) {
        List<Post> postList = postService.getPostsByDate(date);

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
    }

    @GetMapping("/byTag")
    public ResponseEntity<?> findPostsByTag(
            @RequestParam Integer offset,
            @RequestParam Integer limit,
            @RequestParam String tag
    ) {
        List<Post> postList = postService.findPostsByTag(tag);

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
    }

    @GetMapping("/moderation")
    public ResponseEntity<?> getPostsByNeedModeration(@RequestParam Integer offset,
                                                                @RequestParam Integer limit,
                                                                @RequestParam String status
    ) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        List<Post> postList = postService.getPostsByNeedModeration(
                status, userService.findUserById(authService.getUserIdBySession(sessionId)));

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(@RequestParam Integer offset,
                                        @RequestParam Integer limit,
                                        @RequestParam String status
    ) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        List<Post> postList = postService.getMyPostsByStatus(authService.getUserIdBySession(sessionId), status);

        List<PostDto> dtos = getPostDtoList(postList, offset, limit);

        return ResponseEntity.ok(new PostListDto(postList.size(), dtos));
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
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        ResultTrueFalseDto isLiked = postVoteService.votePost(
                votePostIdDto.getPostId(), authService.getUserIdBySession(sessionId), 1);
        return isLiked.getResult() ? ResponseEntity.ok(isLiked) : ResponseEntity.badRequest().body(isLiked);
    }

    @PostMapping("/dislike")
    public ResponseEntity<?> dislike(@RequestBody VotePostIdDto votePostIdDto) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
            throw new UnauthorizedException();
        }

        ResultTrueFalseDto isDisliked = postVoteService.votePost(
                votePostIdDto.getPostId(), authService.getUserIdBySession(sessionId), -1);
        return isDisliked.getResult() ? ResponseEntity.ok(isDisliked) : ResponseEntity.badRequest().body(isDisliked);
    }

    private ResponseEntity<?> checkOnErrors(PostPublishDto postPublishDto, Integer postId, String type) {
        String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
        if (!authService.checkAuthorization(sessionId)) {
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

        return savePostAndTags(sessionId, postPublishDto, postId, type);
    }

    private ResponseEntity<?> savePostAndTags(String sessionId, PostPublishDto postPublishDto, Integer id, String type) {
        User author = userService.findUserById(authService.getUserIdBySession(sessionId));

        Integer postId = type.equals("add") ?
                postService.addPost(postPublishDto, author)
                : postService.editPost(postPublishDto, author, id);

        String[] tags = postPublishDto.getTags();

        List<Integer> tagIds = tagService.saveTags(tags);

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
                post.getTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm")),
                authorDto,
                post.getTitle(),
                post.getText().substring(0, announceLength),
                likeCount,
                dislikeCount,
                commentCount,
                post.getViewCount()
        );
    }

    private List<PostDto> getPostDtoList(List<Post> postList, Integer offset, Integer limit) {
        int subMaxIndex = offset + limit;
        int totalPostCount = postList.size();
        return postList
                .subList(offset, Math.min(subMaxIndex, totalPostCount))
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
