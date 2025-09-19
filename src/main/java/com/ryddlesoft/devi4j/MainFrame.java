package com.ryddlesoft.devi4j;

import io.github.classgraph.ClassInfo;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainFrame extends JFrame {

    private enum VisualizationMode {PACKAGE, CLASS}

    private Project currentProject;
    private final ProjectManager projectManager;
    private DependencyScanner.ScanResultContainer scanResult;

    private JTree fileTree;
    private GraphVisualizer graphVisualizer;
    private VisualizationMode currentMode = VisualizationMode.CLASS;
    private JMenu recentProjectsMenu;
    private static final int MAX_RECENT_PROJECTS = 10;

    private JTabbedPane rightTabbedPane;
    private JTextArea metricsTextArea;
    private CodeMetricsAnalyzer codeMetricsAnalyzer;

    public MainFrame() {
        this.projectManager = new ProjectManager();
        setTitle("DeVi4J Dependency Visualizer");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    public void initialize() {
        this.codeMetricsAnalyzer = new CodeMetricsAnalyzer();
        setupMenuBar();
        setupToolBar();
        setupMainView();
        setVisible(true);
    }

    private void applyTheme(String theme) {
        try {
            if (theme.equals("light")) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else if (theme.equals("dark")) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            // Apply the new theme to all components
            FlatLaf.updateUI();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }   

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newProjectItem = new JMenuItem("New Project...");
        newProjectItem.addActionListener(this::onNewProject);
        fileMenu.add(newProjectItem);
        JMenuItem openProjectItem = new JMenuItem("Open Project...");
        openProjectItem.addActionListener(this::onOpenProject);
        fileMenu.add(openProjectItem);

        recentProjectsMenu = new JMenu("Recent");
        fileMenu.add(recentProjectsMenu);
        updateRecentProjectsMenu();

        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        JMenu editMenu = new JMenu("Edit");
        JMenuItem editProjectItem = new JMenuItem("Edit Project...");
        editProjectItem.addActionListener(this::onEditProject);
        editMenu.add(editProjectItem);
        menuBar.add(editMenu);
        JMenu aboutMenu = new JMenu("About");
        JMenuItem aboutItem = new JMenuItem("About DeVi4J...");
        aboutItem.addActionListener(this::onAbout);
        aboutMenu.add(aboutItem);
        menuBar.add(aboutMenu);
        setJMenuBar(menuBar);
    }

    private void setupToolBar() {
        JToolBar toolBar = new JToolBar();
        ButtonGroup modeGroup = new ButtonGroup();

        JToggleButton classModeButton = new JToggleButton("Class", UIManager.getIcon("FileView.fileIcon"));
        classModeButton.setActionCommand(VisualizationMode.CLASS.name());
        classModeButton.setSelected(true);

        JToggleButton packageModeButton = new JToggleButton("Package", UIManager.getIcon("FileView.directoryIcon"));
        packageModeButton.setActionCommand(VisualizationMode.PACKAGE.name());

        modeGroup.add(classModeButton);
        modeGroup.add(packageModeButton);
        toolBar.add(classModeButton);
        toolBar.add(packageModeButton);

        // Agregar un "glue" que empuja todo lo siguiente a la derecha
        toolBar.add(Box.createHorizontalGlue());

        JToggleButton themeSwitchButton = new JToggleButton("🌞");//🌙
        themeSwitchButton.setSelected(false);
        themeSwitchButton.addActionListener(e -> {
            if (!themeSwitchButton.isSelected()) {
                themeSwitchButton.setText("🌙");
                applyTheme("light");
            } else {
                themeSwitchButton.setText("🌞");
                applyTheme("dark");
            }
        });
        toolBar.add(themeSwitchButton);

        // Botones a la derecha
        toolBar.add(themeSwitchButton);

        ActionListener modeListener = e -> {
            currentMode = VisualizationMode.valueOf(e.getActionCommand());
            updateGraphFromSelection();
        };

        classModeButton.addActionListener(modeListener);
        packageModeButton.addActionListener(modeListener);

        add(toolBar, BorderLayout.NORTH);
    }

    private void setupMainView() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("No project loaded");
        fileTree = new JTree(root);
        fileTree.getSelectionModel().addTreeSelectionListener(this::onTreeSelectionChanged);
        JScrollPane treeScrollPane = new JScrollPane(fileTree);

        graphVisualizer = new GraphVisualizer();
        metricsTextArea = new JTextArea();
        metricsTextArea.setEditable(false);
        metricsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane metricsScrollPane = new JScrollPane(metricsTextArea);

        rightTabbedPane = new JTabbedPane();
        rightTabbedPane.addTab("Dependencies", graphVisualizer);
        rightTabbedPane.addTab("Code Metrics", metricsScrollPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, rightTabbedPane);
        splitPane.setDividerLocation(350);

        add(splitPane, BorderLayout.CENTER);
    }

    private void onNewProject(ActionEvent e) {
        ProjectDialog dialog = new ProjectDialog(this, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            this.currentProject = dialog.getProject();
            try {
                projectManager.saveProject(currentProject);
                loadProject(currentProject);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving project: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onOpenProject(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Project");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("DeVi4J Projects (*.devi4j)", "devi4j"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            loadProject(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onEditProject(ActionEvent e) {
        if (currentProject == null) {
            JOptionPane.showMessageDialog(this, "No project is currently open.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ProjectDialog dialog = new ProjectDialog(this, currentProject);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            this.currentProject = dialog.getProject();
            try {
                projectManager.saveProject(currentProject);
                loadProject(currentProject);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving project: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onAbout(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "DeVi4J - A Simple Dependency Visualizer\nVersion: 1.0", "About DeVi4J", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadProject(Project project) {
        this.currentProject = project;
        setTitle("DeVi4J - " + currentProject.getName());
        DependencyScanner scanner = new DependencyScanner();
        this.scanResult = scanner.scan(project.getJarPaths());
        updateFileTree();
        graphVisualizer.updateGraph(new SingleGraph("Empty"), Collections.emptyList()); // Clear graph
        addProjectToRecentList(project.getProjectFilePath());
        updateRecentProjectsMenu();
    }

    private void loadProject(String projectPath) {
        try {
            Project project = projectManager.loadProject(projectPath);
            loadProject(project);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error opening project: " + ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
            removeProjectFromRecentList(projectPath);
            updateRecentProjectsMenu();
        }
    }

    private void updateFileTree() {
        if (scanResult == null) return;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(currentProject.getName());
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        // Sort package names to ensure parents are created before children
        java.util.List<String> sortedPackageNames = new ArrayList<>(scanResult.getPackageContents().keySet());
        Collections.sort(sortedPackageNames);

        for (String packageName : sortedPackageNames) {
            // Find parent node
            int lastDot = packageName.lastIndexOf('.');
            DefaultMutableTreeNode parentNode;
            if (lastDot == -1) {
                parentNode = root;
            } else {
                String parentPackageName = packageName.substring(0, lastDot);
                parentNode = packageNodes.get(parentPackageName);
                // If parent isn't found for some reason, default to root
                if (parentNode == null) {
                    parentNode = root;
                }
            }

            // Create current package node and add it to the map and the parent
            DefaultMutableTreeNode packageNode = new DefaultMutableTreeNode(packageName);
            packageNodes.put(packageName, packageNode);
            parentNode.add(packageNode);
        }

        // Add class nodes to their respective packages
        for (Map.Entry<String, Set<ClassInfo>> entry : scanResult.getPackageContents().entrySet()) {
            String packageName = entry.getKey();
            DefaultMutableTreeNode packageNode = packageNodes.get(packageName);
            if (packageNode != null) {
                for (ClassInfo classInfo : entry.getValue()) {
                    DefaultMutableTreeNode classNode = new DefaultMutableTreeNode(classInfo);
                    packageNode.add(classNode);
                }
            }
        }

        fileTree.setModel(new DefaultTreeModel(root));
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                if (userObject instanceof ClassInfo) {
                    setText(((ClassInfo) userObject).getSimpleName());
                }
                return this;
            }
        });
    }

    private void onTreeSelectionChanged(TreeSelectionEvent e) {
        updateGraphFromSelection();
        updateMetricsFromSelection();
    }

    private void updateGraphFromSelection() {
        TreePath[] selectionPaths = fileTree.getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length == 0 || scanResult == null) {
            graphVisualizer.updateGraph(new SingleGraph("Empty"), Collections.emptyList());
            return;
        }

        if (currentMode == VisualizationMode.PACKAGE) {
            visualizePackages(selectionPaths);
        }
        else {
            visualizeClasses(selectionPaths);
        }
    }

    private void visualizePackages(TreePath[] selectionPaths) {
        Graph graph = graphVisualizer.createStyledGraph("PackageGraph");
        Map<String, Set<String>> allDeps = scanResult.getPackageDependencies();
        Set<String> selectedPackages = new HashSet<>();
        Set<String> allAvailablePackages = allDeps.keySet();

        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (selectedNode.isRoot()) {
                selectedPackages.addAll(allAvailablePackages);
                break;
            }
            if (selectedNode.getUserObject() instanceof String) {
                String selectedPackageName = (String) selectedNode.getUserObject();
                for (String pkgName : allAvailablePackages) {
                    if (pkgName.equals(selectedPackageName) || pkgName.startsWith(selectedPackageName + ".")) {
                        selectedPackages.add(pkgName);
                    }
                }
            }
        }

        for (String pkgName : selectedPackages) {
            org.graphstream.graph.Node node = graph.addNode(pkgName);
            node.setAttribute("ui.label", pkgName);
            node.setAttribute("ui.class", "package");
        }

        for (String origin : selectedPackages) {
            for (String target : allDeps.getOrDefault(origin, Collections.emptySet())) {
                if (selectedPackages.contains(target)) {
                    graph.addEdge(origin + "->" + target, origin, target, true);
                }
            }
        }

        CycleDetector cycleDetector = new CycleDetector(allDeps);
        java.util.List<java.util.List<String>> cycles = cycleDetector.findCycles(selectedPackages);

        graphVisualizer.applyHeuristicLayout(graph);
        graphVisualizer.updateGraph(graph, cycles);
    }

    private void visualizeClasses(TreePath[] selectionPaths) {
        Graph graph = graphVisualizer.createStyledGraph("ClassGraph");
        Set<ClassInfo> selectedClasses = new HashSet<>();

        // Helper function to get the correct simple name, especially for anonymous classes
        java.util.function.Function<ClassInfo, String> getCorrectSimpleName = ci -> {
            String fqcn = ci.getName();
            int lastDot = fqcn.lastIndexOf('.');
            return (lastDot == -1) ? fqcn : fqcn.substring(lastDot + 1);
        };

        for (TreePath path : selectionPaths) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (selectedNode.isRoot()) {
                scanResult.getPackageContents().values().forEach(selectedClasses::addAll);
                break;
            }
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof String) { // Package selected
                String selectedPackageName = (String) userObject;
                for (Map.Entry<String, Set<ClassInfo>> entry : scanResult.getPackageContents().entrySet()) {
                    if (entry.getKey().equals(selectedPackageName) || entry.getKey().startsWith(selectedPackageName + ".")) {
                        selectedClasses.addAll(entry.getValue());
                    }
                }
            } else if (userObject instanceof ClassInfo) { // Class selected
                selectedClasses.add((ClassInfo) userObject);
            }
        }

        Map<String, ClassInfo> classMap = selectedClasses.stream().collect(Collectors.toMap(ClassInfo::getName, c -> c));

        // Create nodes with correct names
        for(ClassInfo ci : selectedClasses) {
            String nodeName = getCorrectSimpleName.apply(ci);
            org.graphstream.graph.Node node = graph.addNode(nodeName);
            node.setAttribute("ui.label", nodeName);
        }

        // Create edges with filtering
        for (ClassInfo origin : selectedClasses) {
            String originNodeName = getCorrectSimpleName.apply(origin);

            for (ClassInfo target : origin.getClassDependencies()) {
                if (classMap.containsKey(target.getName())) {
                    String targetNodeName = getCorrectSimpleName.apply(target);

                    // Skip self-references
                    if (originNodeName.equals(targetNodeName)) {
                        continue;
                    }

                    // **** FIX: Filter out dependency from inner class to its outer class ****
                    int dollarIndex = originNodeName.lastIndexOf('$');
                    if (dollarIndex != -1) {
                        String outerClassName = originNodeName.substring(0, dollarIndex);
                        if (outerClassName.equals(targetNodeName)) {
                            continue; // Skip this dependency
                        }
                    }

                    if(graph.getNode(originNodeName) != null && graph.getNode(targetNodeName) != null) {
                        graph.addEdge(originNodeName + "->" + targetNodeName, originNodeName, targetNodeName, true);
                    }
                }
            }
        }

        // Create a map for cycle detection using the correct simple names
        Map<String, Set<String>> classDependencies = new HashMap<>();
        for (ClassInfo origin : selectedClasses) {
            String originName = getCorrectSimpleName.apply(origin);
            Set<String> deps = origin.getClassDependencies().stream()
                    .filter(c -> classMap.containsKey(c.getName()))
                    .map(getCorrectSimpleName)
                    .collect(Collectors.toSet());
            classDependencies.put(originName, deps);
        }
        CycleDetector cycleDetector = new CycleDetector(classDependencies);
        java.util.List<java.util.List<String>> cycles = cycleDetector.findCycles();

        graphVisualizer.applyHeuristicLayout(graph);
        graphVisualizer.updateGraph(graph, cycles);
    }

    private void updateMetricsFromSelection() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) fileTree.getLastSelectedPathComponent();

        if (selectedNode == null || !(selectedNode.getUserObject() instanceof ClassInfo)) {
            metricsTextArea.setText("");
            return;
        }

        ClassInfo classInfo = (ClassInfo) selectedNode.getUserObject();
        Optional<File> sourceFile = findSourceFileForClass(classInfo);

        if (sourceFile.isPresent()) {
            ClassMetrics metrics = codeMetricsAnalyzer.analyze(sourceFile.get().getAbsolutePath());
            if (metrics != null) {
                metricsTextArea.setText(formatMetrics(metrics));
                metricsTextArea.setCaretPosition(0);
                //rightTabbedPane.setSelectedIndex(1); // Switch to metrics tab
            } else {
                metricsTextArea.setText("Could not analyze file: ".concat(sourceFile.get().getName()));
            }
        } else {
            metricsTextArea.setText("Source file not found for class: ".concat(classInfo.getSimpleName()));
        }
    }

    private Optional<File> findSourceFileForClass(ClassInfo classInfo) {
        if (currentProject == null || currentProject.getSourcePaths() == null) {
            return Optional.empty();
        }

        String relativePath = classInfo.getName().replace('.', File.separatorChar) + ".java";

        for (String sourceRootPath : currentProject.getSourcePaths()) {
            File sourceFile = new File(sourceRootPath, relativePath);
            if (sourceFile.exists()) {
                return Optional.of(sourceFile);
            }
        }
        return Optional.empty();
    }

    private String formatMetrics(ClassMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Metrics for class: ").append(metrics.getFullyQualifiedName()).append("\n\n");
        sb.append("----------------------------------------\n");
        sb.append("METHODS\n");
        sb.append("----------------------------------------\n");

        if (metrics.getMethodMetrics() == null || metrics.getMethodMetrics().isEmpty()) {
            sb.append("No methods found.\n");
        } else {
            for (MethodMetrics method : metrics.getMethodMetrics()) {
                sb.append(String.format("Method: %s\n", method.getMethodName()));
                sb.append(String.format("  - Cyclomatic Complexity: %d\n", method.getCyclomaticComplexity()));
                sb.append(String.format("  - Lines of Code (Statements): %d\n", method.getLineCount()));
                sb.append(String.format("  - Parameters: %d\n", method.getParameterCount()));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private List<String> getRecentProjects() {
        Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
        return IntStream.range(0, MAX_RECENT_PROJECTS)
                .mapToObj(i -> prefs.get("recentProject" + i, null))
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
    }

    private void addProjectToRecentList(String projectPath) {
        Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
        java.util.List<String> recentProjects = getRecentProjects();
        recentProjects.remove(projectPath);
        recentProjects.add(0, projectPath);
        java.util.List<String> toSave = recentProjects.stream().limit(MAX_RECENT_PROJECTS).collect(Collectors.toList());

        for (int i = 0; i < toSave.size(); i++) {
            prefs.put("recentProject" + i, toSave.get(i));
        }
        // Clear out old entries
        for (int i = toSave.size(); i < MAX_RECENT_PROJECTS; i++) {
            prefs.remove("recentProject" + i);
        }
    }

    private void removeProjectFromRecentList(String projectPath) {
        Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
        java.util.List<String> recentProjects = getRecentProjects();
        if (recentProjects.remove(projectPath)) {
            for (int i = 0; i < recentProjects.size(); i++) {
                prefs.put("recentProject" + i, recentProjects.get(i));
            }
            prefs.remove("recentProject" + recentProjects.size());
        }
    }

    private void updateRecentProjectsMenu() {
        recentProjectsMenu.removeAll();
        java.util.List<String> recentProjects = getRecentProjects();
        if (recentProjects.isEmpty()) {
            recentProjectsMenu.setEnabled(false);
            return;
        }
        recentProjectsMenu.setEnabled(true);
        for (String projectPath : recentProjects) {
            File projectFile = new File(projectPath);
            JMenuItem menuItem = new JMenuItem(projectFile.getName());
            menuItem.setToolTipText(projectPath);
            menuItem.addActionListener(e -> loadProject(projectPath));
            recentProjectsMenu.add(menuItem);
        }
    }
}