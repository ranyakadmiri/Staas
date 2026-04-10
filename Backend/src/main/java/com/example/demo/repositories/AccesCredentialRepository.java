package com.example.demo.repositories;
import com.example.demo.entities.AccessCredential;
import com.example.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccesCredentialRepository extends JpaRepository<AccessCredential, Long> {
    AccessCredential findByProjectId(Long projectId);
    List<AccessCredential> findByProjectOwnerEmail(String email);}
