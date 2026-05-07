package til.stegmueller.centic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import til.stegmueller.centic.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeycloakId(String keycloakId);
}
