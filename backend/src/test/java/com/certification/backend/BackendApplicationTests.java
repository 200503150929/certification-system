package com.certification.backend;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class BackendApplicationTests {

    @Test
    void mainMethodCanBeReferencedWithoutStartingSpringContext() {
        assertThatCode(() -> {
            Class<?> applicationClass = BackendApplication.class;
            applicationClass.getDeclaredMethod("main", String[].class);
        }).doesNotThrowAnyException();
    }

}
