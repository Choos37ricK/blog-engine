package project.repositories;

import org.springframework.data.repository.CrudRepository;
import project.models.User;

import java.util.Optional;

public interface UsersRepo extends CrudRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCode(String code);
}
