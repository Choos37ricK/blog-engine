package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.ResultTrueFalseDto;
import project.models.PostVote;
import project.repositories.PostsVotesRepo;

import java.util.Date;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PostVoteService {

    private final PostsVotesRepo postsVotesRepo;

    public ResultTrueFalseDto votePost(Integer postId, Integer userId, Integer value) {
        Optional<PostVote> exist = postsVotesRepo.findByPostIdAndUserIdAndValue(postId, userId, value);
        if (exist.isPresent()) {
            return new ResultTrueFalseDto(false);
        }

        exist = postsVotesRepo.findByPostIdAndUserIdAndValue(postId, userId, value == 1 ? -1 : 1);
        exist.ifPresent(postsVotesRepo::delete);

        postsVotesRepo.save(new PostVote(userId, postId, new Date(), value));

        return new ResultTrueFalseDto(true);
    }

    public Integer countVotesByPostIdAndValue(Integer postId, Integer value) {
        return postsVotesRepo.countByPostIdAndValue(postId, value);
    }

    public Integer countVotesByValue(Integer value) {
        return postsVotesRepo.countPostVotesByValue(value);
    }
}
