package project.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import project.models.Image;

import java.awt.*;

@Repository
public interface ImageRepository extends CrudRepository<Image, Integer> {}
