package tn.esprit.users_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.users_service.entity.User;
import tn.esprit.users_service.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setFirstname(userDetails.getFirstname());
            user.setLastname(userDetails.getLastname());
            user.setEmail(userDetails.getEmail());
            user.setPassword(userDetails.getPassword());
            user.setPhone(userDetails.getPhone());
            user.setRole(userDetails.getRole());
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
