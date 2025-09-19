package com.ryddlesoft.devi4j;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DependencyScanner {

    /**
     * A container for the results of a scan.
     */
    public static class ScanResultContainer {
        private final Map<String, Set<String>> packageDependencies = new HashMap<>();
        private final Map<String, Set<ClassInfo>> packageContents = new TreeMap<>(); // Use TreeMap for sorted package names
        private final ScanResult rawScanResult;

        public ScanResultContainer(ScanResult rawScanResult) {
            this.rawScanResult = rawScanResult;
        }

        public Map<String, Set<String>> getPackageDependencies() {
            return packageDependencies;
        }

        public Map<String, Set<ClassInfo>> getPackageContents() {
            return packageContents;
        }

        public ScanResult getRawScanResult() {
            return rawScanResult;
        }

        public Map<String, Set<String>> getClassDependencies() {
            Map<String, Set<String>> classDeps = new HashMap<>();
            java.util.function.Function<String, String> getSimpleName = fqcn -> {
                int lastDot = fqcn.lastIndexOf('.');
                return (lastDot == -1) ? fqcn : fqcn.substring(lastDot + 1);
            };

            for (ClassInfo classInfo : rawScanResult.getAllClasses()) {
                if (!isProjectPackage(classInfo.getPackageName())) {
                    continue;
                }

                String sourceSimpleName = getSimpleName.apply(classInfo.getName());
                classDeps.putIfAbsent(sourceSimpleName, new HashSet<>());

                classInfo.getClassDependencies().forEach(dep -> {
                    if (!isProjectPackage(dep.getPackageName())) {
                        return; // Continue for forEach
                    }

                    String targetSimpleName = getSimpleName.apply(dep.getName());

                    // Filter out dependencies from an inner class to its outer class
                    int dollarIndex = sourceSimpleName.lastIndexOf('$');
                    if (dollarIndex != -1) {
                        String outerClassName = sourceSimpleName.substring(0, dollarIndex);
                        if (outerClassName.equals(targetSimpleName)) {
                            return; // Skip this dependency
                        }
                    }

                    classDeps.get(sourceSimpleName).add(targetSimpleName);
                });
            }
            return classDeps;
        }
    }

    public ScanResultContainer scan(List<String> paths) {
        ScanResult scanResult = new ClassGraph()
                .verbose()
                .enableAllInfo()
                .enableInterClassDependencies()
                .overrideClasspath(paths)
                .scan();

        ScanResultContainer resultContainer = new ScanResultContainer(scanResult);
        processScanResult(resultContainer);
        return resultContainer;
    }

    private void processScanResult(ScanResultContainer resultContainer) {
        ScanResult scanResult = resultContainer.getRawScanResult();
        ClassInfoList allClasses = scanResult.getAllClasses();

        for (ClassInfo classInfo : allClasses) {
            String originPackage = getPackageName(classInfo);

            if (isProjectPackage(originPackage)) {
                // Populate package contents
                resultContainer.getPackageContents().computeIfAbsent(originPackage, k -> new HashSet<>()).add(classInfo);

                // Populate package dependencies
                resultContainer.getPackageDependencies().putIfAbsent(originPackage, new HashSet<>());
                classInfo.getClassDependencies().forEach(dependency -> {
                    String targetPackage = getPackageName(dependency);
                    if (isProjectPackage(targetPackage) && !originPackage.equals(targetPackage)) {
                        resultContainer.getPackageDependencies().get(originPackage).add(targetPackage);
                    }
                });
            }
        }
    }

    private String getPackageName(ClassInfo classInfo) {
        if (classInfo.getPackageName() == null || classInfo.getPackageName().isEmpty()) {
            return "(default)";
        }
        return classInfo.getPackageName();
    }

    private static boolean isProjectPackage(String packageName) {
        // Simple heuristic to filter out JDK and other common libraries.
        // This can be improved later.
        return !packageName.startsWith("java.") &&
               !packageName.startsWith("javax.") &&
               !packageName.startsWith("sun.") &&
               !packageName.startsWith("com.sun.");
    }
}