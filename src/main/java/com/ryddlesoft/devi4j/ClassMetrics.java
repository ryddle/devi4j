package com.ryddlesoft.devi4j;

import java.util.ArrayList;
import java.util.List;

public class ClassMetrics {
    private String fullyQualifiedName;
    private List<MethodMetrics> methodMetrics;

    public ClassMetrics(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.methodMetrics = new ArrayList<>();
    }

    public void addMethodMetrics(MethodMetrics metrics) {
        this.methodMetrics.add(metrics);
    }

    // Getters and Setters
    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
    }

    public List<MethodMetrics> getMethodMetrics() {
        return methodMetrics;
    }

    public void setMethodMetrics(List<MethodMetrics> methodMetrics) {
        this.methodMetrics = methodMetrics;
    }
}
