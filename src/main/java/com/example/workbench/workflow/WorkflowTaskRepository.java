package com.example.workbench.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {
    List<WorkflowTask> findByWorkflowIdOrderByIdAsc(Long workflowId);
    void deleteByWorkflowId(Long workflowId);
}
