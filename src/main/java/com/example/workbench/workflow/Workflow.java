package com.example.workbench.workflow;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="workflows")
public class Workflow {
    public enum Status { PLANNED, WAITING_APPROVAL, RUNNING, COMPLETED, FAILED, REJECTED, NEEDS_CLARIFICATION, STOPPED, ROLLBACK_REQUESTED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, length=180) public String name;
    @Column(nullable=false, length=6000) public String requirement;
    @Column(nullable=false, length=40) public String scenario;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) public Status status = Status.PLANNED;
    @Column(nullable=false) public boolean approvalRequired;
    @Column(nullable=false) public boolean safeStop = true;
    @Column(nullable=false, length=40) public String planSource = "DETERMINISTIC_FALLBACK";
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    public Workflow() {}
}
