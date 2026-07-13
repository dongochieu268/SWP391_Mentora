package com.edunac.mentora.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LecturerLearningPathBuilderTemplateStructureTest {

    @Test
    void builderDoesNotExposeNodeContentManagementActions() throws IOException {
        for (String path : List.of(
                "templates/lecturer/learning-path/builder.html",
                "templates/lecturer/learning-path/builder-flow.html")) {
            String template = resource(path);

            assertFalse(template.contains("@{/lecturer/nodes/{nid}/contents"),
                    "Builder template should not link to node content management: " + path);
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing template resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
