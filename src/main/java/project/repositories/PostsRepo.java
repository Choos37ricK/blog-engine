package project.repositories;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import project.models.Post;
import project.models.User;
import project.models.enums.ModerationStatusesEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostsRepo extends CrudRepository<Post, Integer> {
    List<Post> findAllByTimeBeforeAndIsActiveAndModerationStatus(
            LocalDateTime time,
            Byte isActive,
            ModerationStatusesEnum status,
            Sort sort
    );

    List<Post> findAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
            String title,
            String text,
            LocalDateTime time,
            Byte isActive,
            ModerationStatusesEnum status
    );

    @Query(value =
            "SELECT * FROM posts " +
            "WHERE date(time) = :date " +
            "AND is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' ", nativeQuery = true)
    List<Post> findAllByTime_DateAndIsActiveAndModerationStatus(@Param("date") String date);

    Optional<Post> findByIdAndIsActiveAndModerationStatus(Integer id, Byte isActive, ModerationStatusesEnum status);

    List<Post> findAllByModerationStatusAndModeratorAndIsActive(
            ModerationStatusesEnum status,
            User moderator,
            Byte isActive
    );

    List<Post> findAllByModerationStatusAndIsActive(ModerationStatusesEnum status, Byte isActive);

    List<Post> findAllByModerationStatusAndIsActiveAndAuthorId(ModerationStatusesEnum status, Byte isActive, Integer authorId);

    List<Post> findAllByIsActiveAndAuthorId(Byte isActive, Integer authorId);

    @Query(
            "select p from Post p " +
            "join Post2Tag p2t on p2t.postId = p.id " +
            "join Tag t on t.id = p2t.tagId " +
            "where t.name = :tagName")
    List<Post> findAllByTag(String tagName);

    @Query(value =
            "select count(*) from posts " +
            "where date(time) = :date " +
            "and is_active = 1 " +
            "and moderation_status = 'ACCEPTED'", nativeQuery = true)
    Integer countByTime(@Param("date") String date);

    Integer countByIsActiveAndModerationStatus(Byte isActive, ModerationStatusesEnum status);

    Integer countPostsByAuthorId(Integer authorId);

    @Query(
            "select count(p) from Post p " +
            "join PostVote pv on pv.postId = p.id " +
            "where p.author.id = :authorId " +
            "and pv.value = :value"
    )
    Integer countTotalVotesByAuthorIdAndValue(Integer authorId, Integer value);

    @Query(
            "select sum(p.viewCount) from Post p " +
            "where p.author.id = :authorId"
    )
    Integer countViewCountByAuthorId(Integer authorId);

    @Query(
            "select sum(p.viewCount) from Post p"
    )
    Integer countViewCount();

    @Query(
            "select min(p.time) from Post p " +
            "where p.author.id = :authorId")
    Optional<LocalDateTime> findByAuthorIdFirstPublicationDate(Integer authorId);

    @Query("select min(p.time) from Post p")
    Optional<LocalDateTime> findFirstPublicationDate();

    Optional<Post> findByIdAndIsActive(Integer postId, Byte isActive);

    @Query("select distinct year(p.time) from Post p")
    List<Integer> findAllDistinctByTimeYear();

    @Query(value =
            "SELECT * FROM posts " +
            "WHERE year(time) = :year " +
            "AND is_active = 1 " +
            "AND moderation_status = 'ACCEPTED' " +
            "AND time <= now()", nativeQuery = true)
    List<Post> findAllByYear(@Param("year") Integer year);
}
