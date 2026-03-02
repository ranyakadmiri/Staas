package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level= AccessLevel.PRIVATE)
@Entity
public class BucketQuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long maxSizeGB;

    private Long maxObjects;

    private Long usedSizeGB = 0L;

    @OneToOne
    @JoinColumn(name = "bucket_id")
    private Bucket bucket;
}
