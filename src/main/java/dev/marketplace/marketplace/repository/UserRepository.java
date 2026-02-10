package dev.marketplace.marketplace.repository;

import dev.marketplace.marketplace.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.city WHERE u.id = :id")
    Optional<User> findByIdWithCity(@Param("id") UUID id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.city WHERE u.email = :email")
    Optional<User> findByEmailWithCity(@Param("email") String email);
}

