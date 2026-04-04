package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.users_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
