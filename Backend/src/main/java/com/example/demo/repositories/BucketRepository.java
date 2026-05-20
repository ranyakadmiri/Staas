package com.example.demo.repositories;
import com.example.demo.entities.Bucket;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BucketRepository extends JpaRepository<Bucket, Long> {
    List<Bucket> findByProjectId(Long projectId);
    Optional<Bucket> findByNameAndProjectId(String name, Long projectId);
}
