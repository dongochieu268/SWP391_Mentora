package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.*;
import com.edunac.mentora.dto.AssessmentForm;
import com.edunac.mentora.dto.QuestionForm;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.assessment.QuestionBankService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
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
    private final QuestionBankService questionBankService;
    private final SubjectService subjectService;

    public LecturerAssessmentController(AssessmentService assessmentService,
                                        QuestionBankService questionBankService,
                                        SubjectService subjectService) {
        this.assessmentService = assessmentService;
        this.questionBankService = questionBankService;
        this.subjectService = subjectService;
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

    @GetMapping("/{id}/bank-questions")
    @ResponseBody
    public BankQuestionSearchResponse searchBankQuestions(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        var result = questionBankService.searchAvailableForAssessment(
                id, keyword, difficulty, page, size, currentUser(session));
        var items = result.getContent().stream()
                .map(question -> new BankQuestionSearchItem(
                        question.getId(),
                        question.getContent(),
                        question.getDifficulty(),
                        question.getDefaultScore()))
                .toList();
        return new BankQuestionSearchResponse(
                items,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements(),
                result.isFirst(),
                result.isLast()
        );
    }

    @GetMapping("/{id}/bank-questions/random-preview")
    @ResponseBody
    public ResponseEntity<?> previewRandomQuestions(
            @PathVariable Integer id,
            @RequestParam Integer count,
            @RequestParam(defaultValue = "") String difficulty,
            HttpSession session
    ) {
        try {
            var items = questionBankService.previewRandom(id, count, difficulty, currentUser(session)).stream()
                    .map(question -> new BankQuestionSearchItem(
                            question.getId(),
                            question.getContent(),
                            question.getDifficulty(),
                            question.getDefaultScore()))
                    .toList();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        }
    }

    @PostMapping("/{id}/questions/{questionId}/edit")
    public String updateQuestion(@PathVariable Integer id, @PathVariable Integer questionId,
                                 @ModelAttribute QuestionForm form,
                                 HttpSession session, RedirectAttributes ra) {
        try {
            assessmentService.updateQuestion(id, questionId, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật bản sao câu hỏi trong bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/questions/import")
    public String importQuestions(@PathVariable Integer id,
                                  @RequestParam(name = "bankQuestionIds", required = false) List<Integer> bankQuestionIds,
                                  HttpSession session, RedirectAttributes ra) {
        try {
            int count = questionBankService.importSelected(id, bankQuestionIds, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm " + count + " câu hỏi từ ngân hàng.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/questions/random")
    public String importRandom(@PathVariable Integer id,
                               @RequestParam(name = "bankQuestionIds", required = false) List<Integer> bankQuestionIds,
                               @RequestParam(defaultValue = "false") boolean shuffleAnswers,
                               HttpSession session, RedirectAttributes ra) {
        try {
            int count = questionBankService.importRandomSelected(
                    id, bankQuestionIds, shuffleAnswers, currentUser(session));
            ra.addFlashAttribute("success", "Đã random và thêm " + count + " câu hỏi vào bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/questions/save-to-bank")
    public String saveToBank(@PathVariable Integer id,
                             @RequestParam(name = "questionIds", required = false) List<Integer> questionIds,
                             HttpSession session, RedirectAttributes ra) {
        try {
            int count = questionBankService.saveAssessmentQuestions(id, questionIds, currentUser(session));
            ra.addFlashAttribute("success", "Đã lưu " + count + " câu hỏi vào ngân hàng.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Integer id,
            @ModelAttribute AssessmentForm form,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.update(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/questions")
    public String addQuestion(
            @PathVariable Integer id,
            @ModelAttribute QuestionForm form,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.addQuestion(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/questions/{questionId}/delete")
    public String deleteQuestion(
            @PathVariable Integer id,
            @PathVariable Integer questionId,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.deleteQuestion(id, questionId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
    }

    @PostMapping("/{id}/publish")
    public String publish(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes ra
    ) {
        try {
            assessmentService.publish(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã publish bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/assessments/" + id;
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
        model.addAttribute("activeSubjects", subjectService.getActiveSubjects());
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

    public record BankQuestionSearchItem(Integer id, String content, String difficulty,
                                         BigDecimal defaultScore) {
    }

    public record BankQuestionSearchResponse(List<BankQuestionSearchItem> items, int page, int totalPages,
                                             long totalElements, boolean first, boolean last) {
    }

    public record ApiError(String message) {
    }
}
