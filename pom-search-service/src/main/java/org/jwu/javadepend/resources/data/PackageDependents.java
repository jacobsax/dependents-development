package org.jwu.javadepend.resources.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PackageDependents {
    @JsonProperty("total_count")
    public int totalCount;

    @JsonProperty("incomplete_results")
    public boolean incompleteResults;

    @JsonProperty("projects")
    public List<ProjectInfo> projects;

    public PackageDependents(int totalCount, boolean incompleteResults) {
        this.totalCount = totalCount;
        this.incompleteResults = incompleteResults;

        this.projects = new ArrayList();
    }

    public void addProject(ProjectInfo project) {
        projects.add(project);
    }
}
