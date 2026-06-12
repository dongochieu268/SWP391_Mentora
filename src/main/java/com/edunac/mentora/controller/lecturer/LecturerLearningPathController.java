package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.Assessment;
import com.edunac.mentora.domain.assessment.AssessmentType;
import com.edunac.mentora.domain.assessment.DeliveryMode;
import com.edunac.mentora.dto.AssessmentForm;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.dto.LearningPathForm;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.learningpath.LearningPathService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lecturer/learning-paths")
public class LecturerLearningPathController {

    private final LearningPathService pathService;
    private final SubjectService subjectService;
    private final AssessmentService assessmentService;
    private final BranchRuleService branchRuleService;
    private final PathBuilderViewSupport builderViewSupport;

    public LecturerLearningPathController(LearningPathService pathService,
                                          SubjectService subjectService,
                                          AssessmentService assessmentService,
                                          BranchRuleService branchRuleService,
                                          PathBuilderViewSupport builderViewSupport) {
        this.pathService = pathService;
        this.subjectService = subjectService;
        this.assessmentService = assessmentService;
        this.branchRuleService = branchRuleService;
        this.builderViewSupport = builderViewSupport;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        List<LearningPath> paths = pathService.findByCreator(user);
        Set<Integer> withClassroom = paths.stream()
                .filter(p -> pathService.hasClassroom(p.getId()))
                .map(LearningPath::getId)
                .collect(Collectors.toSet());
        model.addAttribute("paths", paths);
        model.addAttribute("pathsWithClassroom", withClassroom);
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
                         @RequestParam(required = false) Integer addBranch,
                         @RequestParam(required = false) String tag,
                         @RequestParam(required = false) Integer testPanel,
                         HttpSession session, Model model) {
        User user = currentUser(session);
        LearningPath path = pathService.findByIdAndOwner(id, user);
        List<LearningNode> nodes = pathService.getNodes(id);

        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("hasClassroom", pathService.hasClassroom(id));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "learning-paths");
        model.addAttribute("mode", "standalone");

        builderViewSupport.populateBuilderModel(id, nodes, user, model);
        builderViewSupport.populateTestPanel(testPanel, user, model);
        builderViewSupport.populateNodeModal(addAfter, addEnd, editNode, addBranch, tag, nodes, model);

        return "lecturer/learning-path/builder";
    }

    @PostMapping("/{id}/nodes/{nodeId}/create-test")
    public String createTest(@PathVariable Integer id, @PathVariable Integer nodeId,
                             @RequestParam(required = false) String returnTo,
                             HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            pathService.findByIdAndOwner(id, user);
            LearningNode node = pathService.getNodes(id).stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy node."));

            AssessmentForm form = new AssessmentForm();
            form.setTitle(node.getTitle());
            form.setType(AssessmentType.BRANCHING_TEST.name());
            form.setDeliveryMode(DeliveryMode.SELF_PACED.name());
            form.setDurationMinutes(30);
            form.setTotalScore(java.math.BigDecimal.TEN);
            Assessment created = assessmentService.create(form, user);

            pathService.attachAssessment(id, nodeId, created.getId(), 5, user);

            ra.addFlashAttribute("success",
                    "Đã tạo bài test nháp \"" + created.getTitle() + "\" — soạn câu hỏi ở khung bên phải.");
            String base = (returnTo != null && returnTo.startsWith("/lecturer/"))
                    ? returnTo : "/lecturer/learning-paths/" + id;
            return "redirect:" + base + (base.contains("?") ? "&" : "?")
                    + "testPanel=" + created.getId() + "#node-" + nodeId;
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return redirectTarget(returnTo, id, null);
        }
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
                          @RequestParam(required = false) String returnTo,
                          HttpSession session, RedirectAttributes ra) {
        try {
            LearningNode saved = pathService.addNode(id, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã thêm node.");
            return redirectTarget(returnTo, id, "#node-" + saved.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return redirectTarget(returnTo, id, null);
        }
    }

    @PostMapping("/{id}/nodes/{nodeId}/edit")
    public String updateNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                             @ModelAttribute LearningNodeForm form,
                             @RequestParam(required = false) String returnTo,
                             HttpSession session, RedirectAttributes ra) {
        try {
            pathService.updateNode(id, nodeId, form, currentUser(session));
            ra.addFlashAttribute("success", "Đã cập nhật node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id, "#node-" + nodeId);
    }

    @PostMapping("/{id}/nodes/{nodeId}/move")
    public String moveNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                           @RequestParam String dir,
                           @RequestParam(required = false) String returnTo,
                           HttpSession session, RedirectAttributes ra) {
        try {
            pathService.moveNode(id, nodeId, dir, currentUser(session));
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id, "#node-" + nodeId);
    }

    @PostMapping("/{id}/nodes/{nodeId}/delete")
    public String deleteNode(@PathVariable Integer id, @PathVariable Integer nodeId,
                             @RequestParam(required = false) String returnTo,
                             HttpSession session, RedirectAttributes ra) {
        try {
            pathService.deleteNode(id, nodeId, currentUser(session));
            ra.addFlashAttribute("success", "Đã xóa node.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return redirectTarget(returnTo, id, null);
    }

    /** Chỉ chấp nhận returnTo nội bộ khu lecturer (chống open redirect); mặc định quay về trang builder. */
    private String redirectTarget(String returnTo, Integer pathId, String anchor) {
        String base = (returnTo != null && returnTo.startsWith("/lecturer/"))
                ? returnTo
                : "/lecturer/learning-paths/" + pathId;
        return "redirect:" + base + (anchor != null ? anchor : "");
    }

    @PostMapping("/{id}/clone")
    public String clone(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        try {
            LearningPath cloned = pathService.clonePath(id, currentUser(session));
            ra.addFlashAttribute("success", "Đã clone lộ trình thành công.");
            return "redirect:/lecturer/learning-paths/" + cloned.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/learning-paths";
        }
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}