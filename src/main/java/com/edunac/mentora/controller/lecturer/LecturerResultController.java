package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.level.LecturerResultService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/lecturer/classes/{classroomId}/results")
public class LecturerResultController {

    private final LecturerResultService resultService;
    private final ClassroomService classroomService;

    public LecturerResultController(LecturerResultService resultService,
                                    ClassroomService classroomService) {
        this.resultService = resultService;
        this.classroomService = classroomService;
    }

    @GetMapping
    public String classResults(@PathVariable Integer classroomId,
                               HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("results", resultService.getClassResults(classroom));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/class-results";
    }

    @GetMapping("/students/{studentId}")
    public String studentDetail(@PathVariable Integer classroomId,
                                @PathVariable Integer studentId,
                                HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("detail", resultService.getStudentDetail(classroom, studentId));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/student-detail";
    }

    @GetMapping("/attempts/{attemptId}")
    public String attemptDetail(@PathVariable Integer classroomId,
                                @PathVariable Integer attemptId,
                                HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("detail", resultService.getAttemptDetail(classroom, attemptId));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/results/attempt-detail";
    }

    // L6 §3 — xuất CSV bảng điểm của lớp.
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Integer classroomId,
                                            HttpSession session) {
        Classroom classroom = classroomService.findForTeacher(classroomId, currentUser(session));
        String csv = resultService.buildResultsCsv(classroom);
        // BOM để Excel mở đúng tiếng Việt.
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, payload, 0, bom.length);
        System.arraycopy(body, 0, payload, bom.length, body.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ket-qua-lop-" + classroomId + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(payload);
    }

    // L7 — cấp thêm 1 lượt làm bài cho một học sinh trên một level.
    @PostMapping("/students/{studentId}/levels/{levelId}/grants")
    public String grantExtraAttempt(@PathVariable Integer classroomId,
                                    @PathVariable Integer studentId,
                                    @PathVariable Integer levelId,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        try {
            resultService.grantExtraAttempt(classroom, user, studentId, levelId);
            ra.addFlashAttribute("success", "Đã cấp thêm 1 lượt.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/results/students/" + studentId;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
