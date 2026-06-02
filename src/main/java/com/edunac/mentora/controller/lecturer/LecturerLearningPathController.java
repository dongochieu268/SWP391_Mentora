package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.dto.LearningPathForm;
import com.edunac.mentora.service.learningpath.LearningPathService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lecturer/learning-paths")
public class LecturerLearningPathController {

    private final LearningPathService pathService;
    private final SubjectService subjectService;

    public LecturerLearningPathController(LearningPathService pathService, SubjectService subjectService) {
        this.pathService = pathService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("paths", pathService.findByCreator(user));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/list";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("form", new LearningPathForm());
        model.addAttribute("activeSubjects", subjectService.getActiveSubjects());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        return "lecturer/learning-path/form";
    }

    @PostMapping
    public String create(HttpSession session,
                         @ModelAttribute LearningPathForm form,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            LearningPath path = pathService.create(form.getSubjectId(), form.getName(), form.getDescription(), user);
            ra.addFlashAttribute("success", "Đã tạo lộ trình thành công.");
            return "redirect:/lecturer/learning-paths/" + path.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/learning-paths/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,
                         @RequestParam(required = false) Integer addAfter,
                         @RequestParam(required = false) Boolean addEnd,
                         @RequestParam(required = false) Integer editNode,
                         HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);
        List<LearningNode> nodes = pathService.getNodes(id);

        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");

        if (!model.containsAttribute("nodeForm")) {
            if (editNode != null) {
                for (LearningNode n : nodes) {
                    if (n.getId().equals(editNode)) {
                        LearningNodeForm f = new LearningNodeForm();
                        f.setId(n.getId());
                        f.setTitle(n.getTitle());
                        f.setDescription(n.getDescription());
                        if (n.getPrerequisite() != null) {
                            f.setPrerequisiteNodeId(n.getPrerequisite().getId());
                        }
                        model.addAttribute("nodeForm", f);
                        model.addAttribute("editMode", true);
                        model.addAttribute("openNodeModal", true);
                        break;
                    }
                }
            } else if (addAfter != null || Boolean.TRUE.equals(addEnd)) {
                LearningNodeForm f = new LearningNodeForm();
                f.setAfterNodeId(addAfter);
                model.addAttribute("nodeForm", f);
                model.addAttribute("editMode", false);
                model.addAttribute("openNodeModal", true);
            }
        }

        if (!model.containsAttribute("nodeForm")) {
            model.addAttribute("nodeForm", new LearningNodeForm()); 
        }

        return "lecturer/learning-path/detail";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Integer id,
                         @ModelAttribute LearningPathForm form,
                         HttpSession session, RedirectAttributes ra) {
        try {
            pathService.update(id, form.getName(), form.getDescription(), currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            pathService.delete(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa lộ trình.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths";
    }

    @PostMapping("/{id}/nodes")
    public String addNode(@PathVariable Integer id,
                          @ModelAttribute LearningNodeForm form,
                          HttpSession session, RedirectAttributes ra) {
        try {
            pathService.addNode(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/nodes/{nodeId}/edit")
    public String updateNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                              @ModelAttribute LearningNodeForm form,
                              HttpSession session, RedirectAttributes ra) {
        try {
            pathService.updateNode(id, nodeId, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    @PostMapping("/{id}/nodes/{nodeId}/delete")
    public String deleteNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                              HttpSession session, RedirectAttributes ra) {
        try {
            pathService.deleteNode(id, nodeId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/learning-paths/" + id;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
