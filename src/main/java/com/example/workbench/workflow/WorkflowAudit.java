package com.example.workbench.workflow;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="workflow_audit", indexes=@Index(name="idx_audit_workflow", columnList="workflowId"))
public class WorkflowAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long workflowId;
    @Column(nullable=false, length=60) public String eventType;
    @Column(nullable=false, length=2000) public String message;
    @Column(nullable=false) public Instant createdAt;
    @Column(length=120) public String actor = "system";
    public WorkflowAudit() {}
}
