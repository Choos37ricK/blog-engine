package project.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import project.models.User;
import project.repositories.UsersRepo;

@Service
@AllArgsConstructor
public class UserService {

    private final UsersRepo usersRepo;

    public User findUserByEmail(String email) {
        return usersRepo.findByEmail(email).orElse(null);
    }

    public User findUserById(Integer id) {
        return usersRepo.findById(id).orElse(null);
    }
}
