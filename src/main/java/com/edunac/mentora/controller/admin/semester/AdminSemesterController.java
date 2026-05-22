package com.edunac.mentora.controller.admin.semester;

import com.edunac.mentora.domain.semester.Semester;
import com.edunac.mentora.service.semester.SemesterService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/semesters")
public class AdminSemesterController {

    private final SemesterService semesterService;

    public AdminSemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("semesters", semesterService.findAll());
        return "admin/semester/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("semester", new Semester());
        model.addAttribute("isEdit", false);
        return "admin/semester/form";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "ACTIVE") String status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            semesterService.create(name, startDate, endDate, status);
            redirectAttributes.addFlashAttribute("success", "Đã thêm học kỳ mới.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/semesters/new";
        }

        return "redirect:/admin/semesters";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return semesterService.findById(id)
                .map(semester -> {
                    model.addAttribute("semester", semester);
                    model.addAttribute("isEdit", true);
                    return "admin/semester/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy học kỳ.");
                    return "redirect:/admin/semesters";
                });
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            semesterService.update(id, name, startDate, endDate, status);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật học kỳ.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/semesters/" + id + "/edit";
        }

        return "redirect:/admin/semesters";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            semesterService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa học kỳ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/admin/semesters";
    }
}
