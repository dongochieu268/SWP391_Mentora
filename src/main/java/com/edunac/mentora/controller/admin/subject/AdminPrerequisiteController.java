package com.edunac.mentora.controller.admin.subject;

import com.edunac.mentora.service.subject.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        // Môn chính
        model.addAttribute("mainSubject",
                subjectService.findById(subjectId));

        // Danh sách môn có thể thêm làm prerequisite
        model.addAttribute("allSubjects",
                subjectService.getAvailablePrerequisites(subjectId));

        // Danh sách prerequisite hiện tại
        model.addAttribute("prerequisites",
                subjectService.getPrerequisites(subjectId));

        return "subjects/prerequisite";
    }

    @PostMapping("/add")
    public String add(@PathVariable Integer subjectId,
                      @RequestParam Integer prerequisiteId,
                      @RequestParam String type,
                      RedirectAttributes ra) {

        try {

            subjectService.addPrerequisite(
                    subjectId,
                    prerequisiteId,
                    type
            );

            ra.addFlashAttribute(
                    "successMessage",
                    "Đã thêm môn tiên quyết!"
            );

        } catch (Exception e) {

            ra.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/subjects/" +
                subjectId +
                "/prerequisites";
    }

    @PostMapping("/delete/{id}")
    public String remove(@PathVariable Integer subjectId,
                         @PathVariable("id") Integer id,
                         RedirectAttributes ra) {

        try {

            subjectService.removePrerequisite(id);

            ra.addFlashAttribute(
                    "successMessage",
                    "Đã gỡ bỏ môn tiên quyết!"
            );

        } catch (Exception e) {

            ra.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/subjects/" +
                subjectId +
                "/prerequisites";
    }
}