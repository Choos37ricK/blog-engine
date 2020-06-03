package project.services;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import project.dto.CalendarDto;
import project.dto.PostPublishDto;
import project.dto.YearPublicationsDto;
import project.models.IYearPublicationCount;
import project.models.Post;
import project.models.User;
import project.models.enums.ModerationStatusesEnum;
import project.repositories.PostsRepo;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


@Service
@AllArgsConstructor
public class PostService {

    private final PostsRepo postsRepo;

    public Integer addPost(PostPublishDto postPublishDto, User author) {

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

        return postsRepo.save(post).getId();
    }

    public Integer editPost(PostPublishDto postPublishDto, User editor, Integer postId) {
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

    public List<Post> getPostsBySort(String mode) {
        List<Post> postList = null;
        switch (mode) {
            case "recent":
                postList = postsRepo.findAllByTimeBeforeAndIsActiveAndModerationStatus(
                        LocalDateTime.now(),
                        (byte) 1,
                        ModerationStatusesEnum.ACCEPTED,
                        Sort.by(Sort.Direction.DESC, "time")
                );
                break;
            case "popular":

            case "best":

                postList = postsRepo.findAllByModerationStatusAndIsActive(ModerationStatusesEnum.ACCEPTED, (byte) 1);
                break;
            case "early":
                postList = postsRepo.findAllByTimeBeforeAndIsActiveAndModerationStatus(
                        LocalDateTime.now(),
                        (byte) 1,
                        ModerationStatusesEnum.ACCEPTED,
                        Sort.by(Sort.Direction.ASC, "time")
                );
                break;
        }

        return postList;
    }

    public Post getPostByIdAndModerationStatus(Integer postId, Byte isModerator) {

        if (isModerator == 0) {
            return postsRepo
                    .findByIdAndIsActiveAndModerationStatus(postId, (byte) 1, ModerationStatusesEnum.ACCEPTED)
                    .orElse(null);
        } else {
            return postsRepo.findByIdAndIsActive(postId, (byte) 1).orElse(null);
        }

    }

    public List<Post> getPostsByDate(String date) {
        return postsRepo.findAllByTime_DateAndIsActiveAndModerationStatus(date);
    }

    public List<Post> findPostsByTag(String tag) {
        return postsRepo.findAllByTag(tag);
    }

    public List<Post> getPostsByNeedModeration(String status, User moderator) {
        List<Post> postList = null;

        switch (status) {
            case "new":
                postList = postsRepo.findAllByModerationStatusAndIsActive(ModerationStatusesEnum.NEW, (byte) 1);
                break;
            case "declined":
                postList = postsRepo.findAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.DECLINED, moderator, (byte) 1
                );
                break;
            case "accepted":
                postList = postsRepo.findAllByModerationStatusAndModeratorAndIsActive(
                        ModerationStatusesEnum.ACCEPTED, moderator, (byte) 1
                );
                break;
        }

        return postList;
    }

    public List<Post> findPostsByQuery(String query) {
        return postsRepo.findAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
                query,
                query,
                LocalDateTime.now(),
                (byte) 1,
                ModerationStatusesEnum.ACCEPTED
        );
    }

    public List<Post> getMyPostsByStatus(Integer userId, String status) {
        List<Post> postList = null;
        switch (status) {
            case "inactive":
                postList = postsRepo.findAllByIsActiveAndAuthorId((byte) 0, userId);
                break;
            case "pending":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(ModerationStatusesEnum.NEW, (byte) 1, userId);
                break;
            case "declined":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(ModerationStatusesEnum.DECLINED, (byte) 1, userId);
                break;
            case "published":
                postList = postsRepo.findAllByModerationStatusAndIsActiveAndAuthorId(ModerationStatusesEnum.ACCEPTED, (byte) 1, userId);
                break;
        }

        return postList;
    }

    private Integer countPostsByDate(String date) {
        return postsRepo.countByTime(date);
    }

    public Integer countPostsNeedModeration() {
        return postsRepo.countByIsActiveAndModerationStatus((byte) 1, ModerationStatusesEnum.NEW);
    }

    public Integer countPosts() {
        return postsRepo.countByIsActiveAndModerationStatus((byte) 1, ModerationStatusesEnum.ACCEPTED);
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
