# DeVi4J

`devi4j` is a static code analysis tool for Java projects. The application allows developers to visualize dependencies between classes in an interactive graph and calculate key code quality metrics to identify potential areas for improvement.

## Key Features

*   **Dependency Analysis**: Scans a Java project to identify relationships between classes.
*   **Graphical Visualization**: Displays dependencies in an interactive graph where each node represents a class.
*   **Cycle Detection**: Identifies and highlights dependency cycles in the graph.
*   **Code Quality Metrics (early development)**: Calculates and displays important metrics for each class and its methods, such as:
    *   Cyclomatic Complexity (WMC)
    *   Lack of Cohesion in Methods (LCOM)
    *   Depth of Inheritance Tree (DIT)
    *   Number of Children (NOC)
    *   And more.
*   **Project Explorer**: Navigates the project structure through a tree view.

## Requirements

*   Java JDK (compatible with versions 11 to 24).
*   Apache Maven.

Upon startup, the application will prompt you to select a Java project directory to begin the analysis.
