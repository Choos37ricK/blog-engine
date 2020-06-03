package project.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import project.models.Post2Tag;

import java.util.Optional;

public interface Post2TagRepo extends CrudRepository<Post2Tag, Integer> {

    @Query("select count(p2t) from Post2Tag as p2t " +
            "join Post as p on p.id = p2t.postId " +
            "where p2t.tagId = :tagId " +
            "and p.isActive = 1 " +
            "and p.moderationStatus = 'ACCEPTED' " +
            "and p.time < NOW()")
    Integer countByTagId(Integer tagId);

    Optional<Post2Tag> findByPostIdAndTagId(Integer postId, Integer tagId);
}
