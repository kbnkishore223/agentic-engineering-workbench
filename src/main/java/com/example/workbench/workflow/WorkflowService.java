package com.example.workbench.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
public class WorkflowService {
    private final WorkflowRepository workflows;
    private final WorkflowTaskRepository tasks;
    private final WorkflowAuditRepository audits;
    private final WorkflowPlanner planner;
    public WorkflowService(WorkflowRepository workflows, WorkflowTaskRepository tasks, WorkflowAuditRepository audits, WorkflowPlanner planner) { this.workflows=workflows; this.tasks=tasks; this.audits=audits; this.planner=planner; }

    @Transactional
    public WorkflowDtos.WorkflowResponse create(WorkflowDtos.CreateRequest req) {
        Workflow w = new Workflow(); w.name=req.name().trim(); w.requirement=req.requirement().trim(); w.createdAt=Instant.now(); w.updatedAt=w.createdAt;
        w.approvalRequired=Boolean.TRUE.equals(req.approvalRequired()); w.safeStop=req.safeStop()==null || req.safeStop();
        WorkflowPlanner.Plan plan=planner.plan(w.requirement, req.scenario()); w.scenario=plan.scenario(); w.planSource=plan.source(); w.status="AMBIGUOUS".equals(w.scenario)?Workflow.Status.NEEDS_CLARIFICATION:Workflow.Status.PLANNED;
        workflows.save(w); addTasks(w, plan); audit(w.id,"WORKFLOW_CREATED","Workflow created; scenario="+w.scenario+", planner="+w.planSource);
        if (w.status==Workflow.Status.NEEDS_CLARIFICATION) audit(w.id,"CLARIFICATION_REQUIRED","Requirement is ambiguous. Provide clarification before execution.");
        return response(w);
    }

    private void addTasks(Workflow w, WorkflowPlanner.Plan plan) {
        List<WorkflowTask> newTasks=new ArrayList<>();
        for (WorkflowPlanner.TaskSpec spec:plan.tasks()) { WorkflowTask t=new WorkflowTask(); t.workflowId=w.id; t.taskKey=spec.key(); t.title=spec.title(); t.description=spec.description(); t.dependencyKeys=spec.dependencies(); t.parallelizable=spec.parallelizable(); t.status=WorkflowTask.Status.PENDING; t.maxRetries=2; newTasks.add(t); }
        tasks.saveAll(newTasks);
    }
    @Transactional(readOnly=true) public List<WorkflowDtos.WorkflowResponse> all() { return workflows.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList(); }
    @Transactional(readOnly=true) public WorkflowDtos.WorkflowResponse get(Long id) { return response(find(id)); }
    @Transactional(readOnly=true) public List<WorkflowDtos.AuditResponse> audit(Long id) { find(id); return audits.findByWorkflowIdOrderByCreatedAtDesc(id).stream().map(WorkflowDtos.AuditResponse::from).toList(); }
    @Transactional(readOnly=true) public WorkflowDtos.Metrics metrics() {
        List<Workflow> all=workflows.findAll(); return new WorkflowDtos.Metrics(all.size(),count(all,Workflow.Status.RUNNING),count(all,Workflow.Status.WAITING_APPROVAL),count(all,Workflow.Status.COMPLETED),count(all,Workflow.Status.FAILED),count(all,Workflow.Status.STOPPED));
    }
    private long count(List<Workflow> list, Workflow.Status s) { return list.stream().filter(w->w.status==s).count(); }

    @Transactional
    public WorkflowDtos.WorkflowResponse run(Long id) {
        Workflow w=find(id);
        if (w.status==Workflow.Status.NEEDS_CLARIFICATION || "AMBIGUOUS".equals(w.scenario)) { w.status=Workflow.Status.NEEDS_CLARIFICATION; touch(w); audit(id,"RUN_BLOCKED","Workflow is ambiguous; clarify the requirement before running."); return response(w); }
        if (w.status==Workflow.Status.WAITING_APPROVAL && w.approvalRequired) throw new IllegalStateException("Approval is required before execution.");
        if (w.status==Workflow.Status.COMPLETED) throw new IllegalStateException("Workflow is already completed. Use Replan to create a new plan.");
        if (w.status==Workflow.Status.REJECTED) throw new IllegalStateException("Rejected workflow cannot run. Replan it first.");
        execute(w); return response(w);
    }

    private void execute(Workflow w) {
        if (w.approvalRequired) { w.status=Workflow.Status.WAITING_APPROVAL; touch(w); audit(w.id,"APPROVAL_REQUIRED","Plan requires approval before task execution."); return; }
        w.status=Workflow.Status.RUNNING; touch(w); audit(w.id,"WORKFLOW_STARTED","Workflow execution started.");
        List<WorkflowTask> workflowTasks=tasks.findByWorkflowIdOrderByIdAsc(w.id);
        Map<String,WorkflowTask> byKey=new LinkedHashMap<>(); workflowTasks.forEach(t->byKey.put(t.taskKey,t));
        int guard=0;
        while (workflowTasks.stream().anyMatch(t->t.status==WorkflowTask.Status.PENDING) && guard++ < workflowTasks.size()+2) {
            List<WorkflowTask> ready=workflowTasks.stream().filter(t->t.status==WorkflowTask.Status.PENDING && dependenciesComplete(t,byKey)).toList();
            if (ready.isEmpty()) {
                boolean dependencyFailed=workflowTasks.stream().anyMatch(t->t.status==WorkflowTask.Status.FAILED || t.status==WorkflowTask.Status.SKIPPED);
                w.status=w.safeStop?Workflow.Status.STOPPED:Workflow.Status.FAILED; touch(w); audit(w.id,"DEPENDENCY_BLOCKED",dependencyFailed?"One or more dependencies failed; remaining tasks were not run.":"Dependency cycle or missing dependency detected."); return;
            }
            ready.forEach(t->{t.status=WorkflowTask.Status.RUNNING; t.startedAt=Instant.now();}); tasks.saveAll(ready);
            // Independent ready tasks are dispatched together. Their results are applied to JPA entities on this transaction thread.
            List<CompletableFuture<TaskResult>> futures=ready.stream().map(t->CompletableFuture.supplyAsync(()->perform(w,t))).toList();
            List<TaskResult> results=new ArrayList<>();
            for (CompletableFuture<TaskResult> future:futures) { try { results.add(future.join()); } catch (CompletionException e) { results.add(new TaskResult("", rootMessage(e), true)); } }
            for (int i=0;i<ready.size();i++) {
                WorkflowTask t=ready.get(i); TaskResult result=results.get(i); t.attempts++;
                // Deterministic test hook: add [simulate-failure] to the requirement to force a task failure.
                if (result.failed && t.attempts <= t.maxRetries) {
                    t.status=WorkflowTask.Status.PENDING; t.errorMessage=result.error; audit(w.id,"TASK_RETRY","Task "+t.taskKey+" failed; retry "+t.attempts+" of "+t.maxRetries+".");
                } else if (result.failed) {
                    t.status=WorkflowTask.Status.FAILED; t.errorMessage=result.error; t.completedAt=Instant.now(); audit(w.id,"TASK_FAILED","Task "+t.taskKey+" failed after retries: "+result.error);
                } else { t.status=WorkflowTask.Status.COMPLETED; t.output=result.output; t.errorMessage=null; t.completedAt=Instant.now(); audit(w.id,"TASK_COMPLETED","Task "+t.taskKey+" completed."); }
            }
            tasks.saveAll(ready);
            if (workflowTasks.stream().anyMatch(t->t.status==WorkflowTask.Status.FAILED)) {
                workflowTasks.stream().filter(t->t.status==WorkflowTask.Status.PENDING).forEach(t->t.status=WorkflowTask.Status.SKIPPED); tasks.saveAll(workflowTasks);
                w.status=w.safeStop?Workflow.Status.STOPPED:Workflow.Status.FAILED; touch(w); audit(w.id,w.safeStop?"SAFE_STOP":"WORKFLOW_FAILED",w.safeStop?"Execution stopped safely after task failure.":"Execution ended with a task failure."); return;
            }
        }
        w.status=workflowTasks.stream().allMatch(t->t.status==WorkflowTask.Status.COMPLETED)?Workflow.Status.COMPLETED:Workflow.Status.FAILED; touch(w); audit(w.id,"WORKFLOW_FINISHED","Workflow finished with status "+w.status+".");
    }
    private boolean dependenciesComplete(WorkflowTask task, Map<String,WorkflowTask> byKey) {
        if (task.dependencyKeys==null || task.dependencyKeys.isBlank()) return true;
        for (String key:task.dependencyKeys.split(",")) { WorkflowTask dep=byKey.get(key.trim()); if (dep==null || dep.status!=WorkflowTask.Status.COMPLETED) return false; }
        return true;
    }
    private TaskResult perform(Workflow w, WorkflowTask t) {
        if (w.requirement.toLowerCase(Locale.ROOT).contains("[simulate-failure]") && "T3".equals(t.taskKey)) return new TaskResult("","Simulated failure requested by requirement.",true);
        return new TaskResult("Completed by local deterministic executor: "+t.title, null, false);
    }
    private String rootMessage(Throwable e) { Throwable t=e; while(t.getCause()!=null)t=t.getCause(); return t.getMessage()==null?"Task execution failed.":t.getMessage(); }
    private record TaskResult(String output,String error,boolean failed) {}

    @Transactional public WorkflowDtos.WorkflowResponse approve(Long id) { Workflow w=find(id); if(w.status!=Workflow.Status.WAITING_APPROVAL) throw new IllegalStateException("Workflow is not waiting for approval."); w.approvalRequired=false; audit(id,"PLAN_APPROVED","Workflow plan approved."); execute(w); return response(w); }
    @Transactional public WorkflowDtos.WorkflowResponse reject(Long id, String reason) { Workflow w=find(id); w.status=Workflow.Status.REJECTED; touch(w); audit(id,"PLAN_REJECTED",reason==null||reason.isBlank()?"Workflow plan rejected.":"Workflow plan rejected: "+reason); return response(w); }
    @Transactional public WorkflowDtos.WorkflowResponse clarify(Long id, String answer) {
        Workflow w=find(id); if(w.status!=Workflow.Status.NEEDS_CLARIFICATION && !"AMBIGUOUS".equals(w.scenario)) throw new IllegalStateException("This workflow is not waiting for clarification.");
        w.requirement += "\nClarification from requester: "+answer.trim(); WorkflowPlanner.Plan plan=planner.plan(w.requirement,"GREENFIELD"); w.scenario=plan.scenario(); w.planSource=plan.source(); tasks.deleteByWorkflowId(id); addTasks(w,plan); w.status=Workflow.Status.PLANNED; touch(w); audit(id,"REQUIREMENT_CLARIFIED","Clarification recorded and plan regenerated."); return response(w);
    }
    @Transactional public WorkflowDtos.WorkflowResponse replan(Long id) { Workflow w=find(id); WorkflowPlanner.Plan plan=planner.plan(w.requirement,"AUTO"); tasks.deleteByWorkflowId(id); w.scenario=plan.scenario(); w.planSource=plan.source(); w.status="AMBIGUOUS".equals(w.scenario)?Workflow.Status.NEEDS_CLARIFICATION:Workflow.Status.PLANNED; touch(w); addTasks(w,plan); audit(id,"WORKFLOW_REPLANNED","A fresh task plan was generated."); return response(w); }
    @Transactional public WorkflowDtos.WorkflowResponse rollback(Long id) { Workflow w=find(id); w.status=Workflow.Status.ROLLBACK_REQUESTED; touch(w); audit(id,"ROLLBACK_REQUESTED","Rollback was requested. This demo records the request; it does not reverse external side effects."); return response(w); }
    private Workflow find(Long id) { return workflows.findById(id).orElseThrow(()->new IllegalArgumentException("Workflow "+id+" was not found.")); }
    private WorkflowDtos.WorkflowResponse response(Workflow w) { return WorkflowDtos.WorkflowResponse.from(w,tasks.findByWorkflowIdOrderByIdAsc(w.id)); }
    private void touch(Workflow w) { w.updatedAt=Instant.now(); workflows.save(w); }
    private void audit(Long id,String type,String message) { WorkflowAudit a=new WorkflowAudit(); a.workflowId=id; a.eventType=type; a.message=message; a.createdAt=Instant.now(); a.actor="system"; audits.save(a); }
}
