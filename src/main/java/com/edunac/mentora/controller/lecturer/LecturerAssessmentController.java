package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.*;
import com.edunac.mentora.dto.AssessmentForm;
import com.edunac.mentora.dto.QuestionForm;
import com.edunac.mentora.service.assessment.AssessmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/lecturer/assessments")
public class LecturerAssessmentController {

    private final AssessmentService assessmentService;

    public LecturerAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        List<Assessment> assessments = assessmentService.findByCreator(user);

        // Tách thành tab: Bản nháp (DRAFT) và Đã xuất bản (PUBLISHED + ARCHIVED).
        List<Assessment> drafts = new ArrayList<>();
        List<Assessment> published = new ArrayList<>();
        for (Assessment a : assessments) {
            if (AssessmentStatus.DRAFT.name().equals(a.getStatus())) {
                drafts.add(a);
            } else {
                published.add(a);
            }
        }
        model.addAttribute("draftAssessments", drafts);
        model.addAttribute("publishedAssessments", published);

        // Cần cho modal "Tạo bài test" ngay trên trang danh sách.
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", defaultAssessmentForm());
        }
        addCommonModel(model, user);
        return "lecturer/assessment/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        User user = currentUser(session);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", defaultAssessmentForm());
        }
        addCommonModel(model, user);
        return "lecturer/assessment/form";
    }

    @PostMapping
    public String create(
            @ModelAttribute AssessmentForm form,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            Assessment assessment = assessmentService.create(form, currentUser(session));
            ra.addFlashAttribute("success", "Đã tạo bài test ở trạng thái DRAFT.");
            return "redirect:/lecturer/assessments/" + assessment.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            // Quay về danh sách và mở lại modal tạo bài test (giữ giá trị đã nhập).
            ra.addFlashAttribute("openCreateModal", true);
            return "redirect:/lecturer/assessments";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, HttpSession session, Model model) {
        User user = currentUser(session);
        Assessment assessment = assessmentService.findByIdAndOwner(id, user);
        var questions = assessmentService.findQuestions(id);

        model.addAttribute("assessment", assessment);
        model.addAttribute("form", assessmentService.toForm(assessment));
        model.addAttribute("questions", questions);
        model.addAttribute("optionsByQuestionId", assessmentService.findOptionsGroupedByQuestion(questions));
        model.addAttribute("canEdit", AssessmentStatus.DRAFT.name().equals(assessment.getStatus()));
        addCommonModel(model, user);
        return "lecturer/assessment/detail";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Integer id,
            @ModelAttribute AssessmentForm form,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.update(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id);
    }

    @PostMapping("/{id}/questions")
    public String addQuestion(
            @PathVariable Integer id,
            @ModelAttribute QuestionForm form,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.addQuestion(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id);
    }

    @PostMapping("/{id}/questions/{questionId}/delete")
    public String deleteQuestion(
            @PathVariable Integer id,
            @PathVariable Integer questionId,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.deleteQuestion(id, questionId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id);
    }

    @PostMapping("/{id}/publish")
    public String publish(
            @PathVariable Integer id,
            @RequestParam(required = false) String returnTo,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.publish(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã publish bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id);
    }

    /** Chỉ chấp nhận returnTo nội bộ khu lecturer (chống open redirect); mặc định về trang bài test. */
    private String redirectTarget(String returnTo, Integer assessmentId) {
        String base = (returnTo != null && returnTo.startsWith("/lecturer/"))
                ? returnTo
                : "/lecturer/assessments/" + assessmentId;
        return "redirect:" + base;
    }

    @PostMapping("/{id}/clone")
    public String cloneAssessment(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            Assessment cloned = assessmentService.cloneAssessment(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã clone bài test thành bản DRAFT mới.");
            return "redirect:/lecturer/assessments/" + cloned.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/assessments/" + id;
        }
    }

    private void addCommonModel(Model model, User user) {
        model.addAttribute("user", user);
        model.addAttribute("activePage", "assessments");
        model.addAttribute("typeOptions", AssessmentType.values());
        model.addAttribute("deliveryModes", List.of(DeliveryMode.SELF_PACED));
        model.addAttribute("difficultyOptions", QuestionDifficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());
    }

    private AssessmentForm defaultAssessmentForm() {
        AssessmentForm form = new AssessmentForm();
        form.setType(AssessmentType.BRANCHING_TEST.name());
        form.setDeliveryMode(DeliveryMode.SELF_PACED.name());
        form.setDurationMinutes(30);
        form.setTotalScore(BigDecimal.TEN);
        return form;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
