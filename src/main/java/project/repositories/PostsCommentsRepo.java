package project.repositories;

import org.springframework.data.repository.CrudRepository;
import project.models.PostComment;

import java.util.List;

public interface PostsCommentsRepo extends CrudRepository<PostComment, Integer> {
    Integer countByPostId(Integer postId);

    List<PostComment> findAllByPostId(Integer postId);
}
