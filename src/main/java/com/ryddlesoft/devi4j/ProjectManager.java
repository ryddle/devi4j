package com.ryddlesoft.devi4j;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ProjectManager {

    private final Gson gson;

    public ProjectManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveProject(Project project) throws IOException {
        try (FileWriter writer = new FileWriter(project.getProjectFilePath())) {
            gson.toJson(project, writer);
        }
    }

    public Project loadProject(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            return gson.fromJson(reader, Project.class);
        }
    }
}
