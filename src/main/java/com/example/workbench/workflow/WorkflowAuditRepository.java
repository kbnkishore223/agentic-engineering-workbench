package com.example.workbench.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkflowAuditRepository extends JpaRepository<WorkflowAudit, Long> { List<WorkflowAudit> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId); }
