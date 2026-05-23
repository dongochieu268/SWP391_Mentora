package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.dto.SubjectForm;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer")
public class LecturerSubjectController {

    private final SubjectService subjectService;

    public LecturerSubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping("/subjects")
    public String subjectsPage(
            @RequestParam(required = false) Integer editId,
            @RequestParam(required = false) Boolean create,
            Model model
    ) {
        populatePage(model, "/lecturer/subjects");

        if (!model.containsAttribute("openModal")) {
            if (editId != null) {
                model.addAttribute("subjectForm", subjectService.toForm(subjectService.findById(editId)));
                model.addAttribute("openModal", true);
            } else if (Boolean.TRUE.equals(create)) {
                model.addAttribute("subjectForm", new SubjectForm());
                model.addAttribute("openModal", true);
            }
        }

        return "subjects/manage";
    }

    @PostMapping("/subjects/save")
    public String saveSubject(
            @Valid @ModelAttribute("subjectForm") SubjectForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstValidationError(bindingResult));
            return "redirect:/lecturer/subjects";
        }

        try {
            subjectService.saveForm(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    form.getId() == null ? "Đã thêm môn học mới!" : "Đã cập nhật môn học!");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() != null ? ex.getReason() : "Không thể lưu môn học.");
        }

        return "redirect:/lecturer/subjects";
    }

    @PostMapping("/subjects/delete/{id}")
    public String deleteSubject(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            subjectService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa môn học.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());
        }
        return "redirect:/lecturer/subjects";
    }

    @PostMapping("/subjects/publish/{id}")
    public String publishSubject(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        subjectService.publish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xuất bản môn học (ACTIVE).");
        return "redirect:/lecturer/subjects";
    }

    @PostMapping("/subjects/unpublish/{id}")
    public String unpublishSubject(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        subjectService.unpublish(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn môn học (HIDDEN).");
        return "redirect:/lecturer/subjects";
    }

    private String firstValidationError(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ.");
    }

    private void populatePage(Model model, String baseUrl) {
        model.addAttribute("pageTitle", "Quản lý môn học");
        model.addAttribute("homeUrl", baseUrl);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("sidebarActive", "subjects");
        model.addAttribute("roleLabel", "Giảng viên");
        model.addAttribute("subjects", subjectService.getAllSubjects());
        if (!model.containsAttribute("subjectForm")) {
            model.addAttribute("subjectForm", new SubjectForm());
        }
    }
}
