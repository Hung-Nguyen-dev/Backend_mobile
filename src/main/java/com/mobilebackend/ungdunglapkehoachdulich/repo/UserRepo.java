package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for user queries and authentication checks.
 */
@Repository
public interface UserRepo extends JpaRepository<User, Integer> {

    /**
     * Tìm user theo email (dùng khi mời bằng email)
     */
    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Check username uniqueness.
     */
    boolean existsByUsername(String username);

    /**
     * Check email uniqueness.
     */
    boolean existsByEmail(String email);

    /**
     * Find user by username or email for login.
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
}
