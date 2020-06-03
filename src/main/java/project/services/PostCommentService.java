package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.PostCommentDto;
import project.models.PostComment;
import project.repositories.PostsCommentsRepo;

import java.util.List;

@Service
@AllArgsConstructor
public class PostCommentService {

    private final PostsCommentsRepo postsCommentsRepo;

    public Integer countByPostId(Integer postId) {
        return postsCommentsRepo.countByPostId(postId);
    }

    public List<PostComment> findAllByPostId(Integer postId) {
        return postsCommentsRepo.findAllByPostId(postId);
    }
}
