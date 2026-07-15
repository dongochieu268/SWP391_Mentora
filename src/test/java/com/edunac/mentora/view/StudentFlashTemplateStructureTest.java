package com.edunac.mentora.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentFlashTemplateStructureTest {

    @Test
    void studentRoadmapAndNodeDetailRenderFlashMessages() throws IOException {
        for (String path : List.of(
                "templates/student/classroom/roadmap.html",
                "templates/student/learning/node-detail.html")) {
            String template = resource(path);

            assertTrue(template.contains("th:if=\"${success}\""),
                    "Template should render success flash message: " + path);
            assertTrue(template.contains("th:if=\"${error}\""),
                    "Template should render error flash message: " + path);
            assertTrue(template.contains("alert-success alert-dismissible fade show py-2"),
                    "Template should use the standard success alert style: " + path);
            assertTrue(template.contains("alert-danger alert-dismissible fade show py-2"),
                    "Template should use the standard error alert style: " + path);
            assertTrue(template.contains("th:text=\"${success}\""),
                    "Template should output the success flash body: " + path);
            assertTrue(template.contains("th:text=\"${error}\""),
                    "Template should output the error flash body: " + path);
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing template resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
