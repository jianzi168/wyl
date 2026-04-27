package com.form.cartesianv2.service.impl;

import java.lang.reflect.Method;

/**
 * Simple test runner for CartesianBuilderServiceTest
 */
public class TestRunner {
    public static void main(String[] args) throws Exception {
        CartesianBuilderServiceTest test = new CartesianBuilderServiceTest();
        test.setUp(); // Call @BeforeEach

        int passed = 0;
        int failed = 0;

        // Run test methods
        Method[] methods = CartesianBuilderServiceTest.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                try {
                    System.out.println("Running: " + method.getName());
                    method.invoke(test);
                    System.out.println("  PASSED: " + method.getName());
                    passed++;
                } catch (Exception e) {
                    System.out.println("  FAILED: " + method.getName() + " - " + e.getCause());
                    failed++;
                }
            }
        }

        System.out.println("\n=== Results ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total: " + (passed + failed));

        if (failed > 0) {
            System.exit(1);
        }
    }
}