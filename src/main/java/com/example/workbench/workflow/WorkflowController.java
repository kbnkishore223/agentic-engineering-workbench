package com.example.workbench.workflow;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) { this.service=service; }
    @GetMapping("/metrics") public Map<String,Object> metrics() { return Map.of("workflows",service.metrics()); }
    @GetMapping("/workflows") public List<WorkflowDtos.WorkflowResponse> all() { return service.all(); }
    @PostMapping("/workflows") public WorkflowDtos.WorkflowResponse create(@Valid @RequestBody WorkflowDtos.CreateRequest req) { return service.create(req); }
    @GetMapping("/workflows/{id}") public WorkflowDtos.WorkflowResponse get(@PathVariable Long id) { return service.get(id); }
    @GetMapping("/workflows/{id}/audit") public List<WorkflowDtos.AuditResponse> audit(@PathVariable Long id) { return service.audit(id); }
    @PostMapping("/workflows/{id}/run") public WorkflowDtos.WorkflowResponse run(@PathVariable Long id) { return service.run(id); }
    @PostMapping("/workflows/{id}/resume") public WorkflowDtos.WorkflowResponse resume(@PathVariable Long id) { return service.run(id); }
    @PostMapping("/workflows/{id}/approve") public WorkflowDtos.WorkflowResponse approve(@PathVariable Long id) { return service.approve(id); }
    @PostMapping("/workflows/{id}/reject") public WorkflowDtos.WorkflowResponse reject(@PathVariable Long id, @RequestBody(required=false) Map<String,String> body) { return service.reject(id,body==null?null:body.get("reason")); }
    @PostMapping("/workflows/{id}/clarify") public WorkflowDtos.WorkflowResponse clarify(@PathVariable Long id, @Valid @RequestBody WorkflowDtos.ClarifyRequest body) { return service.clarify(id,body.answer()); }
    @PostMapping("/workflows/{id}/rollback") public WorkflowDtos.WorkflowResponse rollback(@PathVariable Long id) { return service.rollback(id); }
    @PostMapping("/workflows/{id}/replan") public WorkflowDtos.WorkflowResponse replan(@PathVariable Long id) { return service.replan(id); }
}
