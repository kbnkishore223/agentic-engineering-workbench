package com.example.workbench.workflow;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class WorkflowPlanner {
    public record TaskSpec(String key, String title, String description, String dependencies, boolean parallelizable) {}
    public record Plan(String scenario, String source, List<TaskSpec> tasks) {}

    public Plan plan(String requirement, String requestedScenario) {
        String r = requirement.toLowerCase(Locale.ROOT);
        String scenario = requestedScenario == null || requestedScenario.isBlank() || "AUTO".equalsIgnoreCase(requestedScenario)
                ? (r.contains("existing") || r.contains("modify") || r.contains("brownfield") ? "BROWNFIELD" : r.contains("unclear") || r.contains("ambiguous") || r.contains("not sure") ? "AMBIGUOUS" : "GREENFIELD")
                : requestedScenario.toUpperCase(Locale.ROOT);
        List<TaskSpec> tasks = new ArrayList<>();
        tasks.add(new TaskSpec("T1", "Analyze requirement", "Extract goals, constraints, assumptions and acceptance criteria.", "", true));
        if ("AMBIGUOUS".equals(scenario) || r.contains("unclear") || r.contains("ambiguous")) {
            tasks.add(new TaskSpec("T2", "Identify open questions", "List missing decisions; execution must wait for clarification.", "T1", true));
            tasks.add(new TaskSpec("T3", "Prepare clarification summary", "Summarize assumptions and questions for the requester.", "T2", true));
            return new Plan("AMBIGUOUS", "DETERMINISTIC_FALLBACK", tasks);
        }
        if ("BROWNFIELD".equals(scenario)) {
            tasks.add(new TaskSpec("T2", "Inspect existing implementation", "Review current behavior, APIs, dependencies and regression risk.", "T1", true));
            tasks.add(new TaskSpec("T3", "Design minimal change", "Propose backward-compatible changes and migration steps.", "T2", true));
            tasks.add(new TaskSpec("T4", "Implement change", "Apply the approved change while preserving existing behavior.", "T3", false));
            tasks.add(new TaskSpec("T5", "Run regression checks", "Validate existing flows and new acceptance criteria.", "T4", true));
            tasks.add(new TaskSpec("T6", "Document and report", "Record changed files, results, risks and follow-up actions.", "T5", true));
        } else {
            tasks.add(new TaskSpec("T2", "Design solution", "Define architecture, API contracts, data model and acceptance criteria.", "T1", true));
            tasks.add(new TaskSpec("T3", "Implement backend", "Build service logic, persistence, validation and API endpoints.", "T2", false));
            tasks.add(new TaskSpec("T4", "Implement frontend", "Build the user interface and connect it to the API contracts.", "T2", false));
            tasks.add(new TaskSpec("T5", "Test end-to-end behavior", "Test success paths, invalid input, failure paths and acceptance criteria.", "T3,T4", true));
            tasks.add(new TaskSpec("T6", "Document and report", "Record setup, API usage, verification results and known limitations.", "T5", true));
        }
        return new Plan("BROWNFIELD".equals(scenario) ? "BROWNFIELD" : "GREENFIELD", "DETERMINISTIC_FALLBACK", tasks);
    }
}
