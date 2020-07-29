package project.services;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import project.dto.CalendarDto;
import project.dto.PostPublishDto;
import project.models.Post;
import project.models.User;
import project.models.enums.ModerationStatusesEnum;
import project.repositories.PostsRepo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


@Service
@AllArgsConstructor
public class PostService {

    private final PostsRepo postsRepo;

    public Integer addPost(PostPublishDto postPublishDto, User author, Boolean premoderation) {

        LocalDateTime publishDate = LocalDateTime.parse(postPublishDto.getTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        if (publishDate.isBefore(LocalDateTime.now())) {
            publishDate = LocalDateTime.now();
        }

        Post post = new Post(
                postPublishDto.getActive(),
                author.getIsModerator() == 0 ? ModerationStatusesEnum.NEW : ModerationStatusesEnum.ACCEPTED,
                author,
                publishDate,
                postPublishDto.getTitle(),
                postPublishDto.getText(),
                0
        );

        if (!premoderation) {
            post.setModerationStatus(ModerationStatusesEnum.ACCEPTED);
        }

        return postsRepo.save(post).getId();
    }

    public Integer editPost(PostPublishDto postPublishDto, User editor, Integer postId, Boolean premoderation) {
        Post postFromDb = findPostById(postId);

        LocalDateTime publishDate = LocalDateTime.parse(postPublishDto.getTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        if (publishDate.isBefore(LocalDateTime.now())) {
            publishDate = LocalDateTime.now();
        }

        postFromDb.setTime(publishDate);
        postFromDb.setIsActive(postPublishDto.getActive());
        postFromDb.setTitle(postPublishDto.getTitle());
        postFromDb.setText(postPublishDto.getText());

        if (editor.getIsModerator() == 0) {
            postFromDb.setModerationStatus(ModerationStatusesEnum.NEW);
        }

        if (!premoderation) {
            postFromDb.setModerationStatus(ModerationStatusesEnum.ACCEPTED);
        }

        return postsRepo.save(postFromDb).getId();
    }

    public void savePost(Post post) {
        postsRepo.save(post);
    }

    public Post findPostById(Integer postId) {
        return postsRepo.findById(postId).orElse(null);
    }

    public List<Integer> findPublicationYears() {
        return postsRepo.findAllDistinctByTimeYear();
    }

    public CalendarDto getCalendarDto(Integer year) {
        List<Integer> yearsWithPublications = findPublicationYears();

        List<Post> postList = postsRepo.findAllByYear(year);

        Map<String, Integer> postMap = new TreeMap<>();
        postList.forEach(post -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String postDate = post.getTime().format(formatter);
            Integer postCount = countPostsByDate(postDate);
            postMap.put(postDate, postCount);
        });

        return new CalendarDto(yearsWithPublications, postMap);
    }

    public List<Post> getPostsBySort(String mode, Integer offset, Integer limit) {
        List<Post> postList = null;

        switch (mode) {
            case "recent":
                postList = postsRepo.findAllByTimeBeforeAndIsActiveAndModerationStatus(
                        LocalDateTime.now(),
                        (byte) 1,
                        ModerationStatusesEnum.ACCEPTED,
                        PageRequest.of(
                                offset / limit,
                                limit,
                                Sort.by(Sort.Direction.DESC, "time")
                        )
                );
                break;
            case "popular":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndCommentsCount(
                        ModerationStatusesEnum.ACCEPTED, (byte) 1, PageRequest.of(offset / limit, limit)
                );
                break;
            case "best":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndLikesCount(
                        ModerationStatusesEnum.ACCEPTED, (byte) 1, PageRequest.of(offset / limit, limit)
                );
                break;
            case "early":
                postList = postsRepo.findAllByTimeBeforeAndIsActiveAndModerationStatus(
                        LocalDateTime.now(),
                        (byte) 1,
                        ModerationStatusesEnum.ACCEPTED,
                        PageRequest.of(
                                offset / limit,
                                limit,
                                Sort.by(Sort.Direction.ASC, "time")
                        )
                );
                break;
        }

        return postList;
    }

    public Post getPostByIdAndModerationStatus(Integer postId, User user) {

        Post post;
        if (user != null) {
            post = postsRepo.findById(postId).orElse(null);

            if (user.getIsModerator() == 0 && post != null) {
                if (!(post.getAuthor().getId().equals(user.getId()) ||
                        post.getIsActive() == (byte) 1 ||
                        post.getModerationStatus() == ModerationStatusesEnum.ACCEPTED)
                ) {
                    post = null;
                }
            }
        } else {
            post = postsRepo
                    .findByIdAndIsActiveAndModerationStatus(postId, (byte) 1, ModerationStatusesEnum.ACCEPTED)
                    .orElse(null);
        }

        if (post != null) {
            if (!(user == null && post.getAuthor().getId().equals(user.getId()))) {
                postsRepo.updateViewCount(post.getId(), post.getViewCount() + 1);
            }
        }
        return post;
    }

    public List<Post> getPostsByDate(String date, Integer offset, Integer limit) {
        return postsRepo.findAllByTime_DateAndIsActiveAndModerationStatus(
                date, PageRequest.of(offset / limit, limit));
    }

    public Integer countPostsByDate(String date) {
        return postsRepo.countByTime(date);
    }

    public List<Post> findPostsByTag(String tag, Integer offset, Integer limit) {
        return postsRepo.findAllByTag(tag, PageRequest.of(offset / limit, limit));
    }

    public Integer countPostsByTag(String tag) {
        return postsRepo.countAllByTag(tag);
    }

    public List<Post> getPostsByNeedModeration(String status, User moderator, Integer offset, Integer limit) {
        List<Post> postList = null;

        switch (status) {
            case "new":
                postList = postsRepo.findAllByModerationStatusAndIsActive(
                        ModerationStatusesEnum.NEW, (byte) 1,
                        PageRequest.of(offset / limit, limit)
                );
                break;
            case "declined":
                postList = postsRepo.findAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.DECLINED, moderator, (byte) 1,
                        PageRequest.of(offset / limit, limit)
                );
                break;
            case "accepted":
                postList = postsRepo.findAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.ACCEPTED, moderator, (byte) 1,
                        PageRequest.of(offset / limit, limit)
                );
                break;
        }

        return postList;
    }

    public Integer countPostsByNeedModeration(String status, User moderator) {
        Integer count = 0;

        switch (status) {
            case "new":
                count = postsRepo.countAllByModerationStatusAndIsActive(ModerationStatusesEnum.NEW, (byte) 1);
                break;
            case "declined":
                count = postsRepo.countAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.DECLINED, moderator, (byte) 1
                );
                break;
            case "accepted":
                count = postsRepo.countAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.ACCEPTED, moderator, (byte) 1
                );
                break;
        }

        return count;
    }

    public List<Post> findPostsByQuery(String query, Integer offset, Integer limit) {
        return postsRepo.findAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
                query,
                query,
                LocalDateTime.now(),
                (byte) 1,
                ModerationStatusesEnum.ACCEPTED,
                PageRequest.of(offset / limit, limit)
        );
    }

    public Integer countPostsByQuery(String query) {
        return postsRepo.countAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
                query,
                query,
                LocalDateTime.now(),
                (byte) 1,
                ModerationStatusesEnum.ACCEPTED
        );
    }

    public List<Post> getMyPostsByStatus(Integer userId, String status, Integer offset, Integer limit) {
        List<Post> postList = null;
        switch (status) {
            case "inactive":
                postList = postsRepo.findAllByIsActiveAndAuthorId(
                        (byte) 0, userId, PageRequest.of(offset / limit, limit)
                );
                break;
            case "pending":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.NEW, (byte) 1, userId, PageRequest.of(offset / limit, limit)
                );
                break;
            case "declined":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.DECLINED, (byte) 1, userId, PageRequest.of(offset / limit, limit)
                );
                break;
            case "published":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.ACCEPTED, (byte) 1, userId, PageRequest.of(offset / limit, limit)
                );
                break;
        }

        return postList;
    }

    public Integer countMyPostsByStatus(Integer userId, String status) {
        Integer count = 0;
        switch (status) {
            case "inactive":
                count = postsRepo.countAllByIsActiveAndAuthorId((byte) 0, userId);
                break;
            case "pending":
                count = postsRepo.countAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.NEW, (byte) 1, userId
                );
                break;
            case "declined":
                count = postsRepo.countAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.DECLINED, (byte) 1, userId
                );
                break;
            case "published":
                count = postsRepo.countAllByModerationStatusAndIsActiveAndAuthorId(
                        ModerationStatusesEnum.ACCEPTED, (byte) 1, userId
                );
                break;
        }

        return count;
    }

    public Integer countPostsNeedModeration() {
        return postsRepo.countByIsActiveAndModerationStatusAndTimeBefore(
                (byte) 1, ModerationStatusesEnum.NEW, LocalDateTime.now());
    }

    public Integer countPosts() {
        return postsRepo.countByIsActiveAndModerationStatusAndTimeBefore(
                (byte) 1, ModerationStatusesEnum.ACCEPTED, LocalDateTime.now());
    }



    public Integer countPostsByAuthorId(Integer authorId) {
        return postsRepo.countPostsByAuthorId(authorId);
    }

    public Integer countTotalVoteCountByAuhtorIdAndValue(Integer authorId, Integer value) {
        return postsRepo.countTotalVotesByAuthorIdAndValue(authorId, value);
    }

    public Integer countViewCountByAuthorId(Integer authorId) {
        return postsRepo.countViewCountByAuthorId(authorId);
    }

    public Integer countViewCount() {
        return postsRepo.countViewCount();
    }

    public LocalDateTime findByAuhtorIdFirstPublicationDate(Integer authorId) {
        return postsRepo.findByAuthorIdFirstPublicationDate(authorId).orElse(null);
    }

    public LocalDateTime findFirstPublicationDate() {
        return postsRepo.findFirstPublicationDate().orElse(null);
    }
}
