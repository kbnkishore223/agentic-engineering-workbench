package com.example.workbench.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class WorkflowDtos {
    private WorkflowDtos() {}
    public record CreateRequest(@NotBlank @Size(max=180) String name, @NotBlank @Size(max=6000) String requirement, String scenario, Boolean approvalRequired, Boolean safeStop) {}
    public record TaskResponse(Long id, String taskKey, String title, String description, List<String> dependencies, String status, int attempts, int maxRetries, boolean parallelizable, String output, String errorMessage, Instant startedAt, Instant completedAt) {
        static TaskResponse from(WorkflowTask t) { List<String> deps = t.dependencyKeys == null || t.dependencyKeys.isBlank() ? List.of() : java.util.Arrays.stream(t.dependencyKeys.split(",")).filter(s -> !s.isBlank()).toList(); return new TaskResponse(t.id,t.taskKey,t.title,t.description,deps,t.status.name(),t.attempts,t.maxRetries,t.parallelizable,t.output,t.errorMessage,t.startedAt,t.completedAt); }
    }
    public record AuditResponse(Long id, String eventType, String message, Instant createdAt, String actor) {
        static AuditResponse from(WorkflowAudit a) { return new AuditResponse(a.id,a.eventType,a.message,a.createdAt,a.actor); }
    }
    public record WorkflowResponse(Long id, String name, String requirement, String scenario, String status, boolean approvalRequired, boolean safeStop, String planSource, Instant createdAt, Instant updatedAt, List<TaskResponse> tasks) {
        static WorkflowResponse from(Workflow w, List<WorkflowTask> tasks) { return new WorkflowResponse(w.id,w.name,w.requirement,w.scenario,w.status.name(),w.approvalRequired,w.safeStop,w.planSource,w.createdAt,w.updatedAt,tasks.stream().map(TaskResponse::from).toList()); }
    }
    public record Metrics(long total, long running, long waitingApproval, long completed, long failed, long stopped) {}
    public record ClarifyRequest(@NotBlank String answer) {}
}
