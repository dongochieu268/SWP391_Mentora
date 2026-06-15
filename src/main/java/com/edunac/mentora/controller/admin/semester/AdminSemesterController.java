package com.edunac.mentora.controller.admin.semester;

import com.edunac.mentora.service.semester.SemesterService;
import jakarta.servlet.http.HttpSession;
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
    public String list(HttpSession session, Model model) {
        model.addAttribute("semesters", semesterService.findAll());
        model.addAttribute("activePage", "semesters");
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        return "admin/semester/list";
    }

    @GetMapping("/new")
    public String createForm() {
        // Form tạo học kỳ giờ là modal trong trang danh sách
        return "redirect:/admin/semesters";
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
            redirectAttributes.addFlashAttribute("openCreateSemester", true);
            redirectAttributes.addFlashAttribute("formName", name);
            redirectAttributes.addFlashAttribute("formStartDate", startDate);
            redirectAttributes.addFlashAttribute("formEndDate", endDate);
        }

        return "redirect:/admin/semesters";
    }

    @GetMapping("/{id}/edit")
    public String editForm() {
        // Form sửa học kỳ giờ là modal trong trang danh sách (điền sẵn từ dữ liệu dòng).
        return "redirect:/admin/semesters";
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
            // Mở lại modal sửa của đúng học kỳ và giữ giá trị đã nhập.
            redirectAttributes.addFlashAttribute("openEditSemester", id);
            redirectAttributes.addFlashAttribute("formName", name);
            redirectAttributes.addFlashAttribute("formStartDate", startDate);
            redirectAttributes.addFlashAttribute("formEndDate", endDate);
            redirectAttributes.addFlashAttribute("formStatus", status);
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
