package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.models.Post2Tag;
import project.repositories.Post2TagRepo;

import java.util.List;

@Service
@AllArgsConstructor
public class Post2TagService {

    private final Post2TagRepo post2TagRepo;

    public Integer countPostsWithTag(Integer tagId) {
        return post2TagRepo.countByTagId(tagId);
    }

    public void savePost2Tag(Integer postId, List<Integer> tagIds) {
        tagIds.forEach(tagId -> {
            Post2Tag exist = post2TagRepo.findByPostIdAndTagId(postId, tagId).orElse(null);

            if (exist == null) {
                post2TagRepo.save(new Post2Tag(postId, tagId));
            }
        });
    }
}
