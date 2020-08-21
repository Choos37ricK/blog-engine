package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.dto.TagDto;
import project.models.Tag;
import project.repositories.TagsRepo;

import java.util.ArrayList;
import java.util.List;

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

        for (String tagName : tagNames) {
            Tag exist = tagsRepo.findByName(tagName).orElse(null);

            if (exist == null) {
                exist = tagsRepo.save(new Tag(tagName));
            }

            tagIds.add(exist.getId());
        }

        return tagIds;
    }

    public List<TagDto> setWeights(List<TagDto> tagDtoList) {
        Float biggestWeight = tagDtoList.get(0).getWeight();
        if (biggestWeight <= 0.0) {
            return null;
        }

        tagDtoList.get(0).setWeight(1f);

        float multiplicationCoefficient = 1/biggestWeight;
        tagDtoList.forEach(tagDto -> {
            Float tagWeight = tagDto.getWeight();
            if (tagWeight < 0.3 && tagWeight != 0) {
                tagWeight *= multiplicationCoefficient;

                if (tagWeight < 0.3) {
                    tagWeight = 0.3f;
                }
                tagDto.setWeight(tagWeight);
            }
        });

        return tagDtoList;
    }
}
