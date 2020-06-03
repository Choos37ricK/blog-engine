package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.AddCommentDto;
import project.models.PostComment;
import project.repositories.PostsCommentsRepo;

import java.time.LocalDateTime;
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

    public PostComment findById(Integer id) {
        return postsCommentsRepo.findById(id).orElse(null);
    }

    public Integer saveComment(AddCommentDto addCommentDto, Integer authorId) {
        PostComment postComment = new PostComment(
                addCommentDto.getParentId(),
                addCommentDto.getPostId(),
                authorId,
                LocalDateTime.now(),
                addCommentDto.getText()
        );

        return postsCommentsRepo.save(postComment).getId();
    }
}
