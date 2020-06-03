package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.models.Tag;
import project.repositories.TagsRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TagService {

    private final TagsRepo tagsRepo;

    public List<Tag> findAllTags() {
        return tagsRepo.findAll();
    }

    public List<Tag> findByStartsWith(String query) {
        return tagsRepo.findByNameStartingWith(query);
    }

    public List<String> findAllByPostId(Integer postId) {
        return tagsRepo.findAllByPostId(postId);
    }

    public List<Integer> saveTags(String[] tagNames) {
        List<Integer> tagIds = new ArrayList<>();

        for (int i = 0; i < tagNames.length; i++) {
            String tagName = tagNames[i];
            Tag exist = tagsRepo.findByName(tagName).orElse(null);

            if (exist == null) {
                exist = tagsRepo.save(new Tag(tagName));
            }

            tagIds.add(exist.getId());
        }

        return tagIds;
    }
}
