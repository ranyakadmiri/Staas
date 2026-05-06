package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long projectId;

    @Column(nullable = false, unique = true)
    private String shareKey; // proj-12-docs

    @Column(nullable = false)
    private String name; // docs

    @Column(nullable = false)
    private String realPath; // /mnt/cephfs/proj-12-docs

    @Column(nullable = false, unique = true)
    private String pseudoPath; // /proj-12-docs

    @Column(nullable = false)
    private String serverIp;

    @Column(nullable = false)
    private Integer exportId;

    @Column(nullable = false)
    private String status; // CREATING, READY, ERROR, DELETING

    private LocalDateTime createdAt;
}