package com.ryddlesoft.devi4j;

import java.util.ArrayList;
import java.util.List;

public class Project {
    private String name;
    private String projectFilePath;
    private List<String> jarPaths;

    public Project(String name) {
        this.name = name;
        this.jarPaths = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectFilePath() {
        return projectFilePath;
    }

    public void setProjectFilePath(String projectFilePath) {
        this.projectFilePath = projectFilePath;
    }

    public List<String> getJarPaths() {
        return jarPaths;
    }

    public void setJarPaths(List<String> jarPaths) {
        this.jarPaths = jarPaths;
    }

    public void addJarPath(String path) {
        if (!jarPaths.contains(path)) {
            jarPaths.add(path);
        }
    }

    public void removeJarPath(String path) {
        jarPaths.remove(path);
    }
}
