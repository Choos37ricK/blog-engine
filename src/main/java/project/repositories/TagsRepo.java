package project.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import project.models.Tag;

import java.util.List;
import java.util.Optional;

public interface TagsRepo extends CrudRepository<Tag, Integer> {
    List<Tag> findAll();

    List<Tag> findByNameStartingWith(String query);

    @Query(
            "select t.name from Tag t " +
            "join Post2Tag as p2t on p2t.tagId = t.id " +
            "where p2t.postId = :postId")
    List<String> findAllByPostId(Integer postId);

    Optional<Tag> findByName(String name);
}
