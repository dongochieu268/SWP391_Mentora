package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.service.classroom.ClassroomNodeStatusService;
import com.edunac.mentora.service.classroom.ClassroomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer/classes/{classroomId}/nodes")
public class LecturerClassroomNodeController {

    private final ClassroomNodeStatusService nodeStatusService;
    private final ClassroomService classroomService;

    public LecturerClassroomNodeController(ClassroomNodeStatusService nodeStatusService,
                                           ClassroomService classroomService) {
        this.nodeStatusService = nodeStatusService;
        this.classroomService = classroomService;
    }

    @GetMapping
    public String list(@PathVariable Integer classroomId,
                       HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("learningPaths",
                classroomService.findPathsForTeacherAndSubject(user, classroom.getSubject().getId()));
        model.addAttribute("nodes", nodeStatusService.getNodesWithStatus(classroomId, user));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/class/nodes";
    }

    @PostMapping("/attach-path")
    public String attachPath(@PathVariable Integer classroomId,
                             @RequestParam Integer learningPathId,
                             HttpSession session, RedirectAttributes ra) {
        try {
            classroomService.attachLearningPath(classroomId, learningPathId, currentUser(session));
            ra.addFlashAttribute("success", "Đã gắn lộ trình học cho lớp.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/nodes";
    }

    @PostMapping("/{nodeId}/toggle")
    public String toggle(@PathVariable Integer classroomId,
                         @PathVariable Integer nodeId,
                         HttpSession session, RedirectAttributes ra) {
        try {
            nodeStatusService.toggleVisibility(classroomId, nodeId, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái hiển thị node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/nodes";
    }

    @PostMapping("/{nodeId}/show")
    public String show(@PathVariable Integer classroomId,
                       @PathVariable Integer nodeId,
                       HttpSession session, RedirectAttributes ra) {
        try {
            nodeStatusService.setVisibility(classroomId, nodeId,
                    NodeVisibilityStatus.VISIBLE.name(), currentUser(session));
            ra.addFlashAttribute("success", "Đã bật hiển thị node cho lớp.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/nodes";
    }

    @PostMapping("/{nodeId}/hide")
    public String hide(@PathVariable Integer classroomId,
                       @PathVariable Integer nodeId,
                       HttpSession session, RedirectAttributes ra) {
        try {
            nodeStatusService.setVisibility(classroomId, nodeId,
                    NodeVisibilityStatus.HIDDEN.name(), currentUser(session));
            ra.addFlashAttribute("success", "Đã ẩn node khỏi lớp.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/nodes";
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
