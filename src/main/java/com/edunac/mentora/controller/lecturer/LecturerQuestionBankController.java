package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.QuestionDifficulty;
import com.edunac.mentora.domain.assessment.QuestionType;
import com.edunac.mentora.dto.QuestionBankForm;
import com.edunac.mentora.service.assessment.QuestionBankService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/lecturer/question-bank")
public class LecturerQuestionBankController {

    private final QuestionBankService questionBankService;
    private final SubjectService subjectService;

    public LecturerQuestionBankController(QuestionBankService questionBankService, SubjectService subjectService) {
        this.questionBankService = questionBankService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer subjectId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String difficulty,
                       @RequestParam(required = false) String type,
                       HttpSession session, Model model) {
        var subjects = subjectService.getActiveSubjects();
        Integer selectedSubjectId = subjectId != null ? subjectId
                : subjects.stream().findFirst().map(s -> s.getId()).orElse(null);
        var questions = questionBankService.findActiveBySubject(selectedSubjectId, keyword, difficulty, type);

        if (!model.containsAttribute("form")) {
            QuestionBankForm form = new QuestionBankForm();
            form.setSubjectId(selectedSubjectId);
            form.setDifficulty(QuestionDifficulty.MEDIUM.name());
            form.setQuestionType(QuestionType.MULTIPLE_CHOICE.name());
            form.setDefaultScore(BigDecimal.ONE);
            model.addAttribute("form", form);
        }
        model.addAttribute("user", currentUser(session));
        model.addAttribute("activePage", "question-bank");
        model.addAttribute("activeSubjects", subjects);
        model.addAttribute("selectedSubjectId", selectedSubjectId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedDifficulty", difficulty);
        model.addAttribute("selectedType", type);
        model.addAttribute("questions", questions);
        model.addAttribute("optionsByQuestionId", questionBankService.findOptionsGrouped(questions));
        model.addAttribute("difficultyOptions", QuestionDifficulty.values());
        model.addAttribute("questionTypes", QuestionType.values());
        return "lecturer/question-bank/list";
    }

    @PostMapping("/questions")
    public String create(@ModelAttribute QuestionBankForm form, HttpSession session, RedirectAttributes ra) {
        try {
            questionBankService.create(form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm câu hỏi vào ngân hàng.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            ra.addFlashAttribute("openQuestionModal", true);
        }
        return "redirect:/lecturer/question-bank?subjectId=" + form.getSubjectId();
    }

    @PostMapping("/questions/{id}/edit")
    public String update(@PathVariable Integer id, @ModelAttribute QuestionBankForm form,
                         HttpSession session, RedirectAttributes ra) {
        try {
            questionBankService.update(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/question-bank?subjectId=" + form.getSubjectId();
    }

    @PostMapping("/questions/{id}/archive")
    public String archive(@PathVariable Integer id, @RequestParam Integer subjectId,
                          HttpSession session, RedirectAttributes ra) {
        try {
            questionBankService.archive(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã lưu trữ câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/question-bank?subjectId=" + subjectId;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
