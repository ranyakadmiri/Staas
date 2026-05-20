package com.example.demo.repositories;
import com.example.demo.entities.UsageRecord;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {
    List<UsageRecord> findByProjectIdAndBillingPeriod(Long projectId, String period);
    List<UsageRecord> findByProjectIdAndBillingPeriodAndBucketIsNull(Long projectId, String period);

    List<UsageRecord> findByBucketIdOrderByRecordedAtDesc(Long bucketId);
}
