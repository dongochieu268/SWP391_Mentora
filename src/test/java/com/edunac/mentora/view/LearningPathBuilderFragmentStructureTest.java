package com.edunac.mentora.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningPathBuilderFragmentStructureTest {

    @Test
    void builderIsSplitIntoFocusedFragmentsWithoutLosingUiContracts() throws IOException {
        String builder = resource("templates/lecturer/learning-path/builder.html");
        String flow = resource("templates/lecturer/learning-path/builder-flow.html");
        String panel = resource("templates/lecturer/learning-path/builder-test-panel.html");
        String modal = resource("templates/lecturer/learning-path/builder-node-modal.html");
        String scripts = resource("templates/lecturer/learning-path/builder-scripts.html");

        assertTrue(builder.contains("builder-flow :: builderFlow(${baseUrl})"));
        assertTrue(builder.contains("builder-test-panel :: testPanel(${baseUrl})"));
        assertTrue(builder.contains("builder-node-modal :: nodeModal(${baseUrl})"));
        assertTrue(builder.contains("builder-scripts :: builderScripts"));

        assertTrue(flow.contains("th:fragment=\"builderFlow(baseUrl)\""));
        assertTrue(flow.contains("/nodes/{nid}/move"));
        assertTrue(flow.contains("addBranch=${node.id}"));

        assertTrue(panel.contains("th:fragment=\"testPanel(baseUrl)\""));
        assertTrue(panel.contains("/questions/{qid}/delete"));
        assertTrue(panel.contains("/publish"));

        assertTrue(modal.contains("th:fragment=\"nodeModal(baseUrl)\""));
        assertTrue(modal.contains("id=\"modalNode\""));
        assertTrue(modal.contains("name=\"returnTo\""));

        assertTrue(scripts.contains("th:fragment=\"builderScripts\""));
        assertTrue(scripts.contains("function pickType(type)"));
        assertTrue(scripts.contains("function addOptionRow()"));
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing template resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
