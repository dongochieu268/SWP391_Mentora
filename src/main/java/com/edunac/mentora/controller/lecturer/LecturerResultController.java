package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.level.LecturerResultService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lecturer/classes/{classroomId}/results")
public class LecturerResultController {

    private final LecturerResultService resultService;
    private final ClassroomService classroomService;

    public LecturerResultController(LecturerResultService resultService,
                                    ClassroomService classroomService) {
        this.resultService = resultService;
        this.classroomService = classroomService;
    }

    @GetMapping
    public String classResults(@PathVariable Integer classroomId,
                               HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("results", resultService.getClassResults(classroom));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/class-results";
    }

    @GetMapping("/students/{studentId}")
    public String studentDetail(@PathVariable Integer classroomId,
                                @PathVariable Integer studentId,
                                HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("detail", resultService.getStudentDetail(classroom, studentId));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/student-detail";
    }

    @GetMapping("/attempts/{attemptId}")
    public String attemptDetail(@PathVariable Integer classroomId,
                                @PathVariable Integer attemptId,
                                HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("detail", resultService.getAttemptDetail(classroom, attemptId));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/attempt-detail";
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
