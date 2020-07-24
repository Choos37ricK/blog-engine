package project.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import project.models.PostVote;

import java.util.Optional;

public interface PostsVotesRepo extends CrudRepository<PostVote, Integer> {

    Optional<PostVote> findByPostIdAndUserId(Integer postId, Integer userId);

    Integer countByPostIdAndValue(Integer postId, Integer value);

    Integer countPostVotesByValue(Integer value);

    @Modifying
    @Transactional
    @Query("UPDATE PostVote pv SET pv.value = :value WHERE pv.postId = :postId AND pv.userId = :userId")
    void updatePostVoteByPostIdAndUserId(Integer postId, Integer userId, Integer value);
}
