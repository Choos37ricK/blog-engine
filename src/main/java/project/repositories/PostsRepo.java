package project.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = :viewCount WHERE p.id = :postId")
    void updateViewCount(Integer postId, Integer viewCount);

    List<Post> findAllByTimeBeforeAndIsActiveAndModerationStatus(
            LocalDateTime time,
            Byte isActive,
            ModerationStatusesEnum status,
            Pageable pageable
    );

    @Query("SELECT p FROM Post p " +
            "JOIN PostComment pc on pc.postId = p.id " +
            "WHERE p.moderationStatus = :status " +
            "AND p.isActive = :isActive " +
            "GROUP BY pc.postId " +
            "ORDER BY count(p) DESC")
    List<Post> findAllByModerationStatusAndIsActiveAndCommentsCount(ModerationStatusesEnum status, Byte isActive, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "JOIN PostVote pv on pv.postId = p.id " +
            "WHERE p.moderationStatus = :status " +
            "AND p.isActive = :isActive " +
            "AND pv.value = 1 " +
            "GROUP BY pv.postId " +
            "ORDER BY count(p) DESC")
    List<Post> findAllByModerationStatusAndIsActiveAndLikesCount(ModerationStatusesEnum status, Byte isActive, Pageable pageable);

    List<Post> findAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
            String title,
            String text,
            LocalDateTime time,
            Byte isActive,
            ModerationStatusesEnum status,
            Pageable pageable
    );

    Integer countAllByTitleContainingOrTextContainingAndTimeBeforeAndIsActiveAndModerationStatus(
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
    List<Post> findAllByTime_DateAndIsActiveAndModerationStatus(@Param("date") String date, Pageable pageable);

    @Query(value =
            "select count(*) from posts " +
                    "where date(time) = :date " +
                    "and is_active = 1 " +
                    "and moderation_status = 'ACCEPTED'", nativeQuery = true)
    Integer countByTime(@Param("date") String date);

    Optional<Post> findByIdAndIsActiveAndModerationStatus(Integer id, Byte isActive, ModerationStatusesEnum status);

    List<Post> findAllByModerationStatusAndModeratorAndIsActive(
            ModerationStatusesEnum status,
            User moderator,
            Byte isActive,
            Pageable pageable
    );

    Integer countAllByModerationStatusAndModeratorAndIsActive(
            ModerationStatusesEnum status,
            User moderator,
            Byte isActive
    );

    List<Post> findAllByModerationStatusAndIsActive(ModerationStatusesEnum status, Byte isActive, Pageable pageable);
    Integer countAllByModerationStatusAndIsActive(ModerationStatusesEnum status, Byte isActive);

    List<Post> findAllByModerationStatusAndIsActiveAndAuthorId(
            ModerationStatusesEnum status,
            Byte isActive,
            Integer authorId,
            Pageable pageable
    );

    Integer countAllByModerationStatusAndIsActiveAndAuthorId(
            ModerationStatusesEnum status,
            Byte isActive,
            Integer authorId
    );

    List<Post> findAllByIsActiveAndAuthorId(Byte isActive, Integer authorId, Pageable pageable);
    Integer countAllByIsActiveAndAuthorId(Byte isActive, Integer authorId);

    @Query(
            "select p from Post p " +
            "join Post2Tag p2t on p2t.postId = p.id " +
            "join Tag t on t.id = p2t.tagId " +
            "where t.name = :tagName")
    List<Post> findAllByTag(String tagName, Pageable pageable);

    @Query("select count(p) from Post p " +
            "join Post2Tag p2t on p2t.postId = p.id " +
            "join Tag t on t.id = p2t.tagId " +
            "where t.name = :tagName")
    Integer countAllByTag(String tagName);

    Integer countByIsActiveAndModerationStatusAndTimeBefore(
            Byte isActive, ModerationStatusesEnum status, LocalDateTime time
    );



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
