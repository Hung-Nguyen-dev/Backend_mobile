package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for legacy posts used in user profile lists.
 */
public interface PostRepo extends JpaRepository<Post, Integer> {
    /**
     * Find posts by location.
     */
    List<Post> findByLocation(String location);

    /**
     * Find posts authored by a user.
     */
    List<Post> findByUserIdOrderByIdDesc(Integer userId);
}
