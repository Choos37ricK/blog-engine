package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.ResultTrueFalseDto;
import project.models.PostVote;
import project.repositories.PostsVotesRepo;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PostVoteService {

    private final PostsVotesRepo postsVotesRepo;

    public ResultTrueFalseDto votePost(Integer postId, Integer userId, Integer value) {
        Optional<PostVote> exist = postsVotesRepo.findByPostIdAndUserId(postId, userId);
        if (exist.isPresent()) {
            postsVotesRepo.updatePostVoteByPostIdAndUserId(postId, userId, value);
        } else {
            postsVotesRepo.save(new PostVote(userId, postId, LocalDateTime.now(), value));
        }

        return new ResultTrueFalseDto(true);
    }

    public Integer countVotesByPostIdAndValue(Integer postId, Integer value) {
        return postsVotesRepo.countByPostIdAndValue(postId, value);
    }

    public Integer countVotesByValue(Integer value) {
        return postsVotesRepo.countPostVotesByValue(value);
    }
}
