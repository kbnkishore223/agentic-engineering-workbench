package com.example.workbench.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkflowRepository extends JpaRepository<Workflow, Long> { List<Workflow> findAllByOrderByCreatedAtDesc(); }
