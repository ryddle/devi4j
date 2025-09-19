package com.ryddlesoft.devi4j;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.CtScanner;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CodeMetricsAnalyzer {

    public ClassMetrics analyze(String filePath) {
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            return null;
        }

        Launcher launcher = new Launcher();
        launcher.addInputResource(sourceFile.getAbsolutePath());
        launcher.getEnvironment().setNoClasspath(true); // Important for analyzing single files
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setComplianceLevel(11);
        CtModel model = launcher.buildModel();

        CtClass<?> ctClass = model.getElements(new TypeFilter<>(CtClass.class)).stream().findFirst().orElse(null);

        if (ctClass == null) {
            return null;
        }

        ClassMetrics classMetrics = new ClassMetrics(ctClass.getQualifiedName());
        // classMetrics.setMethods(new ArrayList<>()); // Use addMethodMetrics instead

        for (CtMethod<?> ctMethod : ctClass.getMethods()) {
            MethodMetrics methodMetrics = new MethodMetrics(ctMethod.getSimpleName());
            methodMetrics.setParameterCount(ctMethod.getParameters().size());

            int loc = 0;
            if (ctMethod.getBody() != null) {
                loc = ctMethod.getBody().getStatements().size();
            }
            methodMetrics.setLineCount(loc);

            int complexity = calculateCyclomaticComplexity(ctMethod);
            methodMetrics.setCyclomaticComplexity(complexity);

            classMetrics.addMethodMetrics(methodMetrics);
        }

        return classMetrics;
    }

    private int calculateCyclomaticComplexity(CtMethod<?> method) {
        if (method.getBody() == null) {
            return 1;
        }

        ComplexityVisitor visitor = new ComplexityVisitor();
        visitor.scan(method.getBody());
        return visitor.getComplexity();
    }

    private static class ComplexityVisitor extends CtScanner {
        private int complexity = 1;

        public int getComplexity() {
            return complexity;
        }

        @Override
        public void visitCtIf(CtIf ifElement) {
            complexity++;
            super.visitCtIf(ifElement);
        }

        @Override
        public void visitCtFor(CtFor forElement) {
            complexity++;
            super.visitCtFor(forElement);
        }

        @Override
        public void visitCtWhile(CtWhile whileElement) {
            complexity++;
            super.visitCtWhile(whileElement);
        }

        @Override
        public void visitCtDo(CtDo doElement) {
            complexity++;
            super.visitCtDo(doElement);
        }

        @Override
        public <S> void visitCtCase(CtCase<S> caseElement) {
            complexity++;
            super.visitCtCase(caseElement);
        }

        // Removed visitCtCatch as it was causing compilation issues

        @Override
        public <T> void visitCtConditional(CtConditional<T> conditional) {
            complexity++;
            super.visitCtConditional(conditional);
        }

        @Override
        public <T> void visitCtBinaryOperator(CtBinaryOperator<T> operator) {
            if (operator.getKind() == BinaryOperatorKind.AND || operator.getKind() == BinaryOperatorKind.OR) {
                complexity++;
            }
            super.visitCtBinaryOperator(operator);
        }
    }
}
