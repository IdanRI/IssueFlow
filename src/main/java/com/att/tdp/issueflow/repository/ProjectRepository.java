package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByDeletedFalse();

    Optional<Project> findByIdAndDeletedFalse(Long id);

    List<Project> findByDeletedTrue();
}
