package project.repositories;

import org.springframework.data.repository.CrudRepository;
import project.models.PostVote;

import java.util.Optional;

public interface PostsVotesRepo extends CrudRepository<PostVote, Integer> {

    Optional<PostVote> findByPostIdAndUserIdAndValue(Integer postId, Integer userId, Integer value);

    Integer countByPostIdAndValue(Integer postId, Integer value);

    Integer countPostVotesByValue(Integer value);
}
