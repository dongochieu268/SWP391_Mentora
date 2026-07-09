package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.dto.ClassroomForm;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.qna.ClassroomQuestionService;
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
    private final ClassroomQuestionService questionService;

    public LecturerClassroomController(
            ClassroomService classroomService,
            SubjectService subjectService,
            SemesterRepository semesterRepository,
            ClassroomQuestionService questionService
    ) {
        this.classroomService = classroomService;
        this.subjectService = subjectService;
        this.semesterRepository = semesterRepository;
        this.questionService = questionService;
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

    @GetMapping("/{id}/qna")
    public String qna(@PathVariable Integer id,
                      HttpSession session,
                      Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(id, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("questions", questionService.findForLecturer(id, user));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/class/qna";
    }

    @PostMapping("/{id}/qna/questions/{questionId}/answer")
    public String answerQuestion(@PathVariable Integer id,
                                 @PathVariable Integer questionId,
                                 @RequestParam String answerContent,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        try {
            questionService.answerAsLecturer(id, questionId, currentUser(session), answerContent);
            ra.addFlashAttribute("success", "Đã gửi phản hồi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + id + "/qna";
    }

    @PostMapping("/{id}/qna/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Integer id,
                                 @PathVariable Integer questionId,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        try {
            questionService.deleteAsLecturer(id, questionId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + id + "/qna";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @RequestParam String status,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            classroomService.updateStatus(id, status, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật lớp học.");
            return "redirect:/lecturer/classes";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/classes/" + id + "/edit";
        }
    }

    // L6 — Kết thúc lớp: chuyển COMPLETED rồi dẫn thẳng tới trang tổng kết.
    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Integer id,
                           HttpSession session,
                           RedirectAttributes ra) {
        try {
            classroomService.complete(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã kết thúc lớp học.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + id + "/results";
    }

    // L6 §4 — mở lại lớp đã kết thúc (khôi phục khi bấm nhầm).
    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Integer id,
                         HttpSession session,
                         RedirectAttributes ra) {
        try {
            classroomService.reopen(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã mở lại lớp học.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + id + "/results";
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
