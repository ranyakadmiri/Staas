package com.example.demo.repositories;
import com.example.demo.entities.BucketQuota;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BucketQuotaRepository extends JpaRepository<BucketQuota, Long> {
}
