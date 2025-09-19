package com.ryddlesoft.devi4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class ProjectDialog extends JDialog {

    private Project project;
    private boolean saved = false;

    private JTextField projectNameField;
    private DefaultListModel<String> jarListModel;
    private JList<String> jarList;
    private DefaultListModel<String> sourceListModel;
    private JList<String> sourceList;

    public ProjectDialog(JFrame parent, Project project) {
        super(parent, "Project Settings", true);
        this.project = project != null ? project : new Project("New Project");

        setLayout(new BorderLayout(10, 10));
        setSize(600, 550);
        setLocationRelativeTo(parent);

        add(createFieldsPanel(), BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);
    }

    private JPanel createFieldsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Project Name
        JPanel namePanel = new JPanel(new BorderLayout(5, 5));
        namePanel.add(new JLabel("Project Name:"), BorderLayout.WEST);
        projectNameField = new JTextField(project.getName());
        namePanel.add(projectNameField, BorderLayout.CENTER);
        panel.add(namePanel, BorderLayout.NORTH);

        // Panel for lists
        JPanel listsPanel = new JPanel();
        listsPanel.setLayout(new GridLayout(2, 1, 10, 10));

        // JARs List
        JPanel jarsPanel = new JPanel(new BorderLayout(5, 5));
        jarsPanel.setBorder(BorderFactory.createTitledBorder("Project JARs"));
        jarListModel = new DefaultListModel<>();
        project.getJarPaths().forEach(jarListModel::addElement);
        jarList = new JList<>(jarListModel);
        jarsPanel.add(new JScrollPane(jarList), BorderLayout.CENTER);

        JPanel jarsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addJarButton = new JButton("Add JAR(s)...");
        addJarButton.addActionListener(e -> onAddJar());
        JButton removeJarButton = new JButton("Remove Selected");
        removeJarButton.addActionListener(e -> onRemoveJar());
        jarsButtonsPanel.add(addJarButton);
        jarsButtonsPanel.add(removeJarButton);
        jarsPanel.add(jarsButtonsPanel, BorderLayout.SOUTH);
        listsPanel.add(jarsPanel);

        // Source Dirs List
        JPanel sourcesPanel = new JPanel(new BorderLayout(5, 5));
        sourcesPanel.setBorder(BorderFactory.createTitledBorder("Source Code Directories"));
        sourceListModel = new DefaultListModel<>();
        if (project.getSourcePaths() != null) {
            project.getSourcePaths().forEach(sourceListModel::addElement);
        }
        sourceList = new JList<>(sourceListModel);
        sourcesPanel.add(new JScrollPane(sourceList), BorderLayout.CENTER);

        JPanel sourcesButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addSourceButton = new JButton("Add Directory...");
        addSourceButton.addActionListener(e -> onAddSource());
        JButton removeSourceButton = new JButton("Remove Selected");
        removeSourceButton.addActionListener(e -> onRemoveSource());
        sourcesButtonsPanel.add(addSourceButton);
        sourcesButtonsPanel.add(removeSourceButton);
        sourcesPanel.add(sourcesButtonsPanel, BorderLayout.SOUTH);
        listsPanel.add(sourcesPanel);

        panel.add(listsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> onSave());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());
        panel.add(saveButton);
        panel.add(cancelButton);
        return panel;
    }

    private void onAddJar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JAR Files", "jar"));
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                jarListModel.addElement(file.getAbsolutePath());
            }
        }
    }

    private void onRemoveJar() {
        List<String> selected = jarList.getSelectedValuesList();
        for (String item : selected) {
            jarListModel.removeElement(item);
        }
    }

    private void onAddSource() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            for (File file : fileChooser.getSelectedFiles()) {
                sourceListModel.addElement(file.getAbsolutePath());
            }
        }
    }

    private void onRemoveSource() {
        List<String> selected = sourceList.getSelectedValuesList();
        for (String item : selected) {
            sourceListModel.removeElement(item);
        }
    }

    private void onSave() {
        project.setName(projectNameField.getText());

        project.getJarPaths().clear();
        for (int i = 0; i < jarListModel.getSize(); i++) {
            project.addJarPath(jarListModel.getElementAt(i));
        }

        // Handle sourcePaths potentially being null
        if (project.getSourcePaths() == null) {
            project.setSourcePaths(new ArrayList<>());
        }
        project.getSourcePaths().clear();
        for (int i = 0; i < sourceListModel.getSize(); i++) {
            project.addSourcePath(sourceListModel.getElementAt(i));
        }

        if (project.getProjectFilePath() == null || project.getProjectFilePath().isEmpty()) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Project File");
            fileChooser.setSelectedFile(new File(project.getName() + ".devi4j"));
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                project.setProjectFilePath(fileChooser.getSelectedFile().getAbsolutePath());
            } else {
                return; // User cancelled save
            }
        }

        saved = true;
        setVisible(false);
    }

    private void onCancel() {
        saved = false;
        setVisible(false);
    }

    public Project getProject() {
        return project;
    }

    public boolean isSaved() {
        return saved;
    }
}