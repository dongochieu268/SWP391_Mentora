package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.assessment.StudentAssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/student/classrooms/{classroomId}/assessments")
public class StudentAssessmentController {

    private final StudentAssessmentService studentAssessmentService;
    private final AssessmentService assessmentService;

    public StudentAssessmentController(
            StudentAssessmentService studentAssessmentService,
            AssessmentService assessmentService
    ) {
        this.studentAssessmentService = studentAssessmentService;
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{assessmentId}/take")
    public String take(
            @PathVariable Integer classroomId,
            @PathVariable Integer assessmentId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            User user = currentUser(session);
            var submitted = studentAssessmentService.findSubmittedAttempt(assessmentId, classroomId, user);
            if (submitted.isPresent()) {
                return redirectToResult(classroomId, submitted.get().getId());
            }

            var attempt = studentAssessmentService.startAttempt(assessmentId, classroomId, user);
            if (AttemptStatus.SUBMITTED.name().equals(attempt.getStatus())) {
                return redirectToResult(classroomId, attempt.getId());
            }

            var questions = assessmentService.findQuestions(assessmentId);
            model.addAttribute("attempt", attempt);
            model.addAttribute("assessment", attempt.getAssessment());
            model.addAttribute("questions", questions);
            model.addAttribute("optionsByQuestionId", assessmentService.findOptionsGroupedByQuestion(questions));
            model.addAttribute("classroomId", classroomId);
            model.addAttribute("user", user);
            model.addAttribute("activePage", "classrooms");
            return "student/assessment/take";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public String submit(
            @PathVariable Integer classroomId,
            @PathVariable Integer attemptId,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            var attempt = studentAssessmentService.submitAttempt(
                    classroomId,
                    attemptId,
                    currentUser(session),
                    parseAnswers(request)
            );
            return redirectToResult(classroomId, attempt.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }
    }

    @GetMapping("/attempts/{attemptId}/result")
    public String result(
            @PathVariable Integer classroomId,
            @PathVariable Integer attemptId,
            HttpSession session,
            Model model,
            RedirectAttributes ra
    ) {
        try {
            User user = currentUser(session);
            var attempt = studentAssessmentService.findResult(classroomId, attemptId, user);
            model.addAttribute("attempt", attempt);
            model.addAttribute("assessment", attempt.getAssessment());
            model.addAttribute("classroomId", classroomId);
            model.addAttribute("user", user);
            model.addAttribute("activePage", "classrooms");
            return "student/assessment/result";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms/" + classroomId + "/roadmap";
        }
    }

    private Map<Integer, Integer> parseAnswers(HttpServletRequest request) {
        Map<Integer, Integer> answers = new HashMap<>();
        for (String paramName : request.getParameterMap().keySet()) {
            if (!paramName.startsWith("answers_")) {
                continue;
            }
            try {
                Integer questionId = Integer.valueOf(paramName.substring("answers_".length()));
                Integer optionId = Integer.valueOf(request.getParameter(paramName));
                answers.put(questionId, optionId);
            } catch (NumberFormatException ignored) {
                // Ignore malformed answer keys; grading validates option ownership.
            }
        }
        return answers;
    }

    private String redirectToResult(Integer classroomId, Integer attemptId) {
        return "redirect:/student/classrooms/" + classroomId
                + "/assessments/attempts/" + attemptId + "/result";
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
