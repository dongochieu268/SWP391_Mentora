package com.edunac.mentora.controller.admin.subject;

import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.service.subject.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/subjects/{subjectId}/prerequisites")
public class AdminPrerequisiteController {

    private final SubjectService subjectService;

    public AdminPrerequisiteController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public String view(@PathVariable Integer subjectId,
                       Model model,
                       jakarta.servlet.http.HttpSession session) {

        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "subjects");
        model.addAttribute("mainSubject", subjectService.findById(subjectId));
        model.addAttribute("allSubjects", subjectService.getAvailablePrerequisites(subjectId));
        model.addAttribute("prerequisites", subjectService.getPrerequisites(subjectId));
        model.addAttribute("subjectsById", subjectService.getAllSubjects().stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity())));

        return "subjects/prerequisite";
    }

    @PostMapping("/add")
    public String add(@PathVariable Integer subjectId,
                      @RequestParam Integer prerequisiteSubjectId,
                      RedirectAttributes ra) {
        try {
            subjectService.addPrerequisite(subjectId, prerequisiteSubjectId);
            ra.addFlashAttribute("successMessage", "Đã thêm môn tiên quyết!");
        } catch (ResponseStatusException e) {
            // Chỉ lấy phần thông điệp (getReason), tránh hiện chuỗi thô "400 BAD_REQUEST ..."
            ra.addFlashAttribute("errorMessage", e.getReason());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể thêm môn tiên quyết, vui lòng thử lại.");
        }
        return "redirect:/admin/subjects/" + subjectId + "/prerequisites";
    }

    @PostMapping("/delete/{prerequisiteSubjectId}")
    public String remove(@PathVariable Integer subjectId,
                         @PathVariable Integer prerequisiteSubjectId,
                         RedirectAttributes ra) {
        try {
            subjectService.removePrerequisite(subjectId, prerequisiteSubjectId);
            ra.addFlashAttribute("successMessage", "Đã gỡ bỏ môn tiên quyết!");
        } catch (ResponseStatusException e) {
            ra.addFlashAttribute("errorMessage", e.getReason());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không thể gỡ bỏ môn tiên quyết, vui lòng thử lại.");
        }
        return "redirect:/admin/subjects/" + subjectId + "/prerequisites";
    }
}
