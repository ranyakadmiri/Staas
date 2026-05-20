package com.example.demo.repositories;

import com.example.demo.entities.ProvisionEvent;
import com.example.demo.entities.Subscription;
import com.example.demo.entities.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository  extends JpaRepository<Subscription, Long> {
    List<Subscription> findByStatus(SubscriptionStatus status);
    Optional<Subscription> findByProjectIdAndStatus(Long projectId, SubscriptionStatus status);
}
