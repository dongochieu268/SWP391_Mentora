package com.edunac.mentora.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LecturerMaterialTemplateStructureTest {

    @Test
    void materialPageExposesCrudQuestionAndContentContracts() throws IOException {
        String template = resource("templates/lecturer/material/list.html");

        assertTrue(template.contains("@{/lecturer/materials}"));
        assertTrue(template.contains("/questions/assign"));
        assertTrue(template.contains("/questions/unassign"));
        assertTrue(template.contains("questionIds"));
        assertTrue(template.contains("selectedMaterial"));
        assertTrue(template.contains("/contents/save"));
        assertTrue(template.contains("/contents/delete"));
        assertTrue(template.contains("contentForm"));
        assertTrue(template.contains("contentEditId"));
        assertTrue(template.contains("mediaFile"));
        assertTrue(template.contains("materialContents"));
        assertTrue(template.contains("ti-edit"));
        assertTrue(template.contains("nav-tabs"));
        assertTrue(template.contains("material-info-pane"));
        assertTrue(template.contains("material-content-pane"));
        assertTrue(template.contains("material-questions-pane"));
        assertTrue(template.contains("material-summary"));
    }

    @Test
    void materialPageDoesNotContainMojibakeText() throws IOException {
        String template = resource("templates/lecturer/material/list.html");

        assertTrue(template.contains("N&#7897;i dung h&#7885;c c&#7911;a material"));
        assertTrue(template.contains("Th&#234;m n&#7897;i dung"));
        assertTrue(template.contains("X&#243;a n&#7897;i dung n&#224;y?"));
        for (String brokenText : new String[]{"\u00c3", "\u00c4", "\u00c6", "\u00e1\u00bb", "\u00e1\u00ba", "\u00c2", "\ufffd"}) {
            assertFalse(template.contains(brokenText), "Template contains mojibake marker: " + brokenText);
        }
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing template resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
