package com.example.workbench.workflow;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="workflow_tasks", indexes=@Index(name="idx_task_workflow", columnList="workflowId"))
public class WorkflowTask {
    public enum Status { PENDING, READY, RUNNING, COMPLETED, FAILED, SKIPPED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public Long workflowId;
    @Column(nullable=false, length=80) public String taskKey;
    @Column(nullable=false, length=220) public String title;
    @Column(length=2000) public String description;
    @Column(length=500) public String dependencyKeys = "";
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) public Status status = Status.PENDING;
    @Column(nullable=false) public int attempts = 0;
    @Column(nullable=false) public int maxRetries = 2;
    @Column(nullable=false) public boolean parallelizable = true;
    @Column(length=3000) public String output;
    @Column(length=2000) public String errorMessage;
    public Instant startedAt;
    public Instant completedAt;
    public WorkflowTask() {}
}
