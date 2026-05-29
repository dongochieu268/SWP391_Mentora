package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.dto.ClassroomForm;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.subject.SubjectService;
import com.edunac.mentora.repository.semester.SemesterRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer/classes")
public class LecturerClassroomController {

    private final ClassroomService classroomService;
    private final SubjectService subjectService;
    private final SemesterRepository semesterRepository;

    public LecturerClassroomController(
            ClassroomService classroomService,
            SubjectService subjectService,
            SemesterRepository semesterRepository
    ) {
        this.classroomService = classroomService;
        this.subjectService = subjectService;
        this.semesterRepository = semesterRepository;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("classes", classroomService.findByTeacher(user));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/class/list";
    }

    @GetMapping("/new")
    public String createForm(HttpSession session, Model model) {
        ClassroomForm form = model.containsAttribute("form")
                ? (ClassroomForm) model.getAttribute("form")
                : new ClassroomForm();
        populateFormPage(session, model, form, false);
        return "lecturer/class/form";
    }

    @PostMapping
    public String create(@ModelAttribute ClassroomForm form,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            Classroom created = classroomService.create(form, currentUser(session));
            ra.addFlashAttribute("success",
                    "Đã tạo lớp \"" + created.getName() + "\". Mã mời: " + created.getInviteCode());
            return "redirect:/lecturer/classes";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return "redirect:/lecturer/classes/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(id, user);
        ClassroomForm form = model.containsAttribute("form")
                ? (ClassroomForm) model.getAttribute("form")
                : classroomService.toForm(classroom);
        populateFormPage(session, model, form, true);
        model.addAttribute("inviteCode", classroom.getInviteCode());
        return "lecturer/class/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @ModelAttribute ClassroomForm form,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            classroomService.update(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật lớp học.");
            return "redirect:/lecturer/classes";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            form.setId(id);
            ra.addFlashAttribute("form", form);
            return "redirect:/lecturer/classes/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            classroomService.delete(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa lớp học.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes";
    }

    private void populateFormPage(HttpSession session, Model model, ClassroomForm form, boolean isEdit) {
        User user = currentUser(session);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", form);
        }
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("activeSubjects", subjectService.getActiveSubjects());
        model.addAttribute("activeSemesters",
                semesterRepository.findByStatusOrderByStartDateDesc("ACTIVE"));
        model.addAttribute("learningPaths",
                classroomService.findPathsForTeacherAndSubject(user, form.getSubjectId()));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
