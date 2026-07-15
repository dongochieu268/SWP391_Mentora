package com.edunac.mentora.view;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LecturerThemeTemplateStructureTest {

    private static final List<String> PAGE_TEMPLATES = List.of(
            "templates/lecturer/dashboard.html",
            "templates/lecturer/assessment/detail.html",
            "templates/lecturer/assessment/form.html",
            "templates/lecturer/assessment/list.html",
            "templates/lecturer/class/form.html",
            "templates/lecturer/class/list.html",
            "templates/lecturer/class/members.html",
            "templates/lecturer/class/nodes.html",
            "templates/lecturer/class/qna.html",
            "templates/lecturer/course-setup/review.html",
            "templates/lecturer/course-setup/step1.html",
            "templates/lecturer/course-setup/step2.html",
            "templates/lecturer/learning/node-contents.html",
            "templates/lecturer/learning/node-levels.html",
            "templates/lecturer/learning-path/builder.html",
            "templates/lecturer/learning-path/form.html",
            "templates/lecturer/learning-path/list.html",
            "templates/lecturer/material/list.html",
            "templates/lecturer/question-bank/list.html",
            "templates/lecturer/results/attempt-detail.html",
            "templates/lecturer/results/class-results.html",
            "templates/lecturer/results/student-detail.html"
    );

    @Test
    void everyLecturerPageLoadsScopedTheme() throws IOException {
        for (String path : PAGE_TEMPLATES) {
            String template = resource(path);

            assertTrue(template.contains("/assets/css/lecturer-theme.css"),
                    "Missing lecturer theme link: " + path);
            assertTrue(template.contains("<body class=\"lecturer-app"),
                    "Missing lecturer app scope: " + path);
        }
    }

    @Test
    void lecturerThemeSupportsResponsiveAndReducedMotionLayouts() throws IOException {
        String stylesheet = resource("static/assets/css/lecturer-theme.css");

        assertTrue(stylesheet.contains(".lecturer-app"));
        assertTrue(stylesheet.contains("@media (max-width: 767.98px)"));
        assertTrue(stylesheet.contains("@media (prefers-reduced-motion: reduce)"));
        assertTrue(stylesheet.contains(".lecturer-app .table-responsive"));
        assertTrue(stylesheet.contains(".lecturer-app .modal-content"));
        assertTrue(stylesheet.contains(".lecturer-app .nav-tabs"));
        assertTrue(stylesheet.contains(".lecturer-dashboard-hero::before"));
        assertTrue(stylesheet.contains(".lecturer-dashboard-hero::after"));
    }

    @Test
    void lecturerSidebarUsesTheSharedWorkspaceTreatment() throws IOException {
        String template = resource("templates/layout/lecturer-sidebar.html");

        assertTrue(template.contains("lecturer-navigation"));
        assertTrue(template.contains("lecturer-sidebar-note"));
    }

    @Test
    void dashboardExposesLecturerWorkspaceActions() throws IOException {
        String template = resource("templates/lecturer/dashboard.html");

        assertTrue(template.contains("lecturer-hero"));
        assertTrue(template.contains("lecturer-hero-actions"));
        assertTrue(template.contains("lecturer-command-deck"));
        assertTrue(template.contains("lecturer-workspace-grid"));
        assertTrue(template.contains("@{/lecturer/learning-paths}"));
        assertTrue(template.contains("@{/lecturer/assessments}"));
        assertTrue(template.contains("@{/lecturer/classes}"));
    }

    @Test
    void classroomListKeepsManagementRoutesInResponsiveCards() throws IOException {
        String template = resource("templates/lecturer/class/list.html");

        assertTrue(template.contains("lecturer-page-heading"));
        assertTrue(template.contains("lecturer-class-grid"));
        assertTrue(template.contains("lecturer-class-card"));
        assertTrue(template.contains("lecturer-empty-state"));
        assertTrue(template.contains("@{/lecturer/classes/wizard}"));
        assertTrue(template.contains("/members"));
        assertTrue(template.contains("/qna"));
        assertTrue(template.contains("/nodes"));
        assertTrue(template.contains("/edit"));
        assertTrue(template.contains("/delete"));
    }

    @Test
    void materialPageKeepsTwoColumnWorkspace() throws IOException {
        String template = resource("templates/lecturer/material/list.html");

        assertTrue(template.contains("lecturer-page-heading"));
        assertTrue(template.contains("lecturer-material-hero"));
        assertTrue(template.contains("lecturer-material-layout"));
        assertTrue(template.contains("lecturer-material-library"));
        assertTrue(template.contains("lecturer-material-item"));
        assertTrue(template.contains("lecturer-material-sidebar"));
        assertTrue(template.contains("lecturer-material-workspace"));
        assertTrue(template.contains("lecturer-material-studio"));
        assertTrue(template.contains("lecturer-workflow-tabs"));
    }

    @Test
    void lecturerLibrariesExposePurposeBuiltBrowsers() throws IOException {
        String paths = resource("templates/lecturer/learning-path/list.html");
        String assessments = resource("templates/lecturer/assessment/list.html");
        String questionBank = resource("templates/lecturer/question-bank/list.html");

        assertTrue(paths.contains("lecturer-path-browser"));
        assertTrue(paths.contains("lecturer-path-accordion"));
        assertTrue(assessments.contains("lecturer-assessment-browser"));
        assertTrue(assessments.contains("lecturer-assessment-tabs"));
        assertTrue(questionBank.contains("lecturer-filter-panel"));
        assertTrue(questionBank.contains("lecturer-question-hero"));
        assertTrue(questionBank.contains("lecturer-question-toolbar"));
        assertTrue(questionBank.contains("lecturer-question-card"));
    }

    private String resource(String path) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
