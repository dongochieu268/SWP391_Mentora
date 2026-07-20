package com.edunac.mentora.controller;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.service.UserService;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.subject.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    private UserService userService;
    private SubjectService subjectService;
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        subjectService = mock(SubjectService.class);
        controller = new DashboardController(
                mock(ClassroomService.class),
                mock(ClassroomMemberService.class),
                userService,
                subjectService
        );
    }

    @Test
    void adminDashboardUsesDatabaseStatistics() {
        User admin = new User();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);
        when(userService.countByRole("STUDENT")).thenReturn(27L);
        when(userService.countByRole("LECTURER")).thenReturn(6L);
        when(subjectService.countSubjects()).thenReturn(9L);
        when(userService.countByStatus("BANNED")).thenReturn(2L);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.adminDashboard(session, model);

        assertEquals("admin/dashboard", view);
        assertSame(admin, model.get("user"));
        assertEquals(27L, model.get("studentCount"));
        assertEquals(6L, model.get("lecturerCount"));
        assertEquals(9L, model.get("subjectCount"));
        assertEquals(2L, model.get("bannedAccountCount"));
        assertEquals("dashboard", model.get("activePage"));
    }
}
