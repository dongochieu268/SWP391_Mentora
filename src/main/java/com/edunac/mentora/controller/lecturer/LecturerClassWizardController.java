package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.Assessment;
import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.ClassWizardState;
import com.edunac.mentora.dto.ClassroomForm;
import com.edunac.mentora.repository.semester.SemesterRepository;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.learningpath.LearningPathService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wizard "Tạo lớp học" 3 bước: nhập thông tin lớp (session) → xây lộ trình (builder)
 * → xem lại & xác nhận (lúc này mới tạo Classroom thật + mã mời).
 */
@Controller
@RequestMapping("/lecturer/classes/wizard")
public class LecturerClassWizardController {

    static final String SESSION_KEY = "classWizard";

    private final ClassroomService classroomService;
    private final LearningPathService pathService;
    private final AssessmentService assessmentService;
    private final BranchRuleService branchRuleService;
    private final SubjectService subjectService;
    private final SemesterRepository semesterRepository;
    private final PathBuilderViewSupport builderViewSupport;

    public LecturerClassWizardController(ClassroomService classroomService,
                                         LearningPathService pathService,
                                         AssessmentService assessmentService,
                                         BranchRuleService branchRuleService,
                                         SubjectService subjectService,
                                         SemesterRepository semesterRepository,
                                         PathBuilderViewSupport builderViewSupport) {
        this.classroomService = classroomService;
        this.pathService = pathService;
        this.assessmentService = assessmentService;
        this.branchRuleService = branchRuleService;
        this.subjectService = subjectService;
        this.semesterRepository = semesterRepository;
        this.builderViewSupport = builderViewSupport;
    }

    // ===== BƯỚC 1: THÔNG TIN LỚP =====

    @GetMapping
    public String step1(HttpSession session, Model model) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);

        model.addAttribute("state", state != null ? state : new ClassWizardState());
        model.addAttribute("resuming", state != null && state.getLearningPathId() != null);
        model.addAttribute("activeSubjects", subjectService.getActiveSubjects());
        model.addAttribute("activeSemesters", semesterRepository.findByStatusOrderByStartDateDesc("ACTIVE"));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/course-setup/step1";
    }

    @PostMapping("/step1")
    public String submitStep1(@RequestParam String name,
                              @RequestParam Integer subjectId,
                              @RequestParam Integer semesterId,
                              HttpSession session, RedirectAttributes ra) {
        if (name == null || name.isBlank() || name.length() > 200) {
            ra.addFlashAttribute("error", "Tên lớp không được để trống và tối đa 200 ký tự.");
            return "redirect:/lecturer/classes/wizard";
        }

        ClassWizardState state = stateOf(session);
        if (state == null) state = new ClassWizardState();

        // Đổi môn học sau khi đã tạo lộ trình → lộ trình cũ không khớp môn, phải gỡ khỏi wizard
        if (state.getLearningPathId() != null
                && state.getSubjectId() != null && !state.getSubjectId().equals(subjectId)) {
            state.setLearningPathId(null);
            ra.addFlashAttribute("error",
                    "Bạn đã đổi môn học nên lộ trình đã tạo trước đó không còn khớp — hãy tạo/chọn lộ trình mới. "
                    + "Lộ trình cũ vẫn nằm trong trang Lộ trình học của bạn.");
        }

        state.setName(name.trim());
        state.setSubjectId(subjectId);
        state.setSemesterId(semesterId);
        session.setAttribute(SESSION_KEY, state);
        return "redirect:/lecturer/classes/wizard/step2";
    }

    // ===== BƯỚC 2: XÂY LỘ TRÌNH =====

    @GetMapping("/step2")
    public String step2(@RequestParam(required = false) Integer addAfter,
                        @RequestParam(required = false) Boolean addEnd,
                        @RequestParam(required = false) Integer editNode,
                        @RequestParam(required = false) Integer addBranch,
                        @RequestParam(required = false) String tag,
                        @RequestParam(required = false) Integer testPanel,
                        HttpSession session, Model model, RedirectAttributes ra) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state == null || !state.step1Done()) {
            return "redirect:/lecturer/classes/wizard";
        }

        model.addAttribute("state", state);
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        model.addAttribute("mode", "wizard");

        if (state.getLearningPathId() == null) {
            model.addAttribute("defaultPathName", "Lộ trình – " + state.getName());
            model.addAttribute("existingPaths",
                    classroomService.findPathsForTeacherAndSubject(user, state.getSubjectId()));
            model.addAttribute("hasPath", false);
            return "lecturer/course-setup/step2";
        }

        LearningPath path;
        try {
            path = pathService.findByIdAndOwner(state.getLearningPathId(), user);
        } catch (Exception e) {
            // Lộ trình đã bị xóa ngoài wizard → cho chọn/tạo lại
            state.setLearningPathId(null);
            session.setAttribute(SESSION_KEY, state);
            ra.addFlashAttribute("error", "Lộ trình của wizard không còn tồn tại — hãy tạo hoặc chọn lại.");
            return "redirect:/lecturer/classes/wizard/step2";
        }

        List<LearningNode> nodes = pathService.getNodes(path.getId());
        model.addAttribute("path", path);
        model.addAttribute("nodes", nodes);
        model.addAttribute("hasClassroom", pathService.hasClassroom(path.getId()));
        model.addAttribute("hasPath", true);

        builderViewSupport.populateBuilderModel(path.getId(), nodes, user, model);
        builderViewSupport.populateTestPanel(testPanel, user, model);
        builderViewSupport.populateNodeModal(addAfter, addEnd, editNode, addBranch, tag, nodes, model);

        return "lecturer/course-setup/step2";
    }

    /** Tạo lộ trình mới cho wizard (môn học khóa theo bước 1). */
    @PostMapping("/step2/path")
    public String createPath(@RequestParam String pathName,
                             @RequestParam(required = false) String pathDescription,
                             HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state == null || !state.step1Done()) {
            return "redirect:/lecturer/classes/wizard";
        }
        try {
            LearningPath path = pathService.create(state.getSubjectId(), pathName, pathDescription, user);
            state.setLearningPathId(path.getId());
            session.setAttribute(SESSION_KEY, state);
            ra.addFlashAttribute("success", "Đã tạo lộ trình. Bây giờ thêm các node bài học và bài test.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/wizard/step2";
    }

    /** Dùng lại một lộ trình có sẵn */
    @PostMapping("/step2/select-path")
    public String selectPath(@RequestParam Integer learningPathId,
                             HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state == null || !state.step1Done()) {
            return "redirect:/lecturer/classes/wizard";
        }
        try {
            LearningPath path = pathService.findByIdAndOwner(learningPathId, user);
            if (!path.getSubject().getId().equals(state.getSubjectId())) {
                throw new IllegalArgumentException("Lộ trình không thuộc môn học đã chọn.");
            }
            state.setLearningPathId(path.getId());
            session.setAttribute(SESSION_KEY, state);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/wizard/step2";
    }

    // ===== BƯỚC 3: XEM LẠI & XÁC NHẬN =====

    @GetMapping("/review")
    public String review(HttpSession session, Model model) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state == null || !state.step1Done()) {
            return "redirect:/lecturer/classes/wizard";
        }
        if (state.getLearningPathId() == null) {
            return "redirect:/lecturer/classes/wizard/step2";
        }

        LearningPath path = pathService.findByIdAndOwner(state.getLearningPathId(), user);
        List<LearningNode> nodes = pathService.getNodes(path.getId());

        model.addAttribute("state", state);
        model.addAttribute("path", path);
        model.addAttribute("semester", semesterRepository.findById(state.getSemesterId()).orElse(null));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");

        long lessonCount = nodes.stream()
                .filter(n -> !"BRANCH_TEST".equals(n.getNodeType()))
                .filter(n -> n.getBranchTag() == null || "MAIN".equals(n.getBranchTag())).count();
        long testCount = nodes.stream().filter(n -> "BRANCH_TEST".equals(n.getNodeType())).count();
        long passCount = nodes.stream().filter(n -> "PASS".equals(n.getBranchTag())).count();
        long failCount = nodes.stream().filter(n -> "FAIL".equals(n.getBranchTag())).count();
        model.addAttribute("nodeCount", nodes.size());
        model.addAttribute("lessonCount", lessonCount);
        model.addAttribute("testCount", testCount);
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);

        model.addAttribute("errors", computeErrors(nodes, user));
        model.addAttribute("warnings", computeWarnings(path.getId(), nodes, user));
        return "lecturer/course-setup/review";
    }

    @PostMapping("/confirm")
    public String confirm(HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state == null || !state.step1Done() || state.getLearningPathId() == null) {
            return "redirect:/lecturer/classes/wizard";
        }

        List<LearningNode> nodes = pathService.getNodes(state.getLearningPathId());
        List<String> errors = computeErrors(nodes, user);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("error", "Chưa thể tạo lớp: " + String.join(" ", errors));
            return "redirect:/lecturer/classes/wizard/review";
        }

        try {
            ClassroomForm form = new ClassroomForm();
            form.setName(state.getName());
            form.setSubjectId(state.getSubjectId());
            form.setSemesterId(state.getSemesterId());
            form.setLearningPathId(state.getLearningPathId());
            form.setStatus("OPEN");
            Classroom created = classroomService.create(form, user);

            session.removeAttribute(SESSION_KEY);
            ra.addFlashAttribute("success",
                    "Đã tạo lớp \"" + created.getName() + "\". Mã mời: " + created.getInviteCode());
            return "redirect:/lecturer/classes";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer/classes/wizard/review";
        }
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam(required = false, defaultValue = "false") boolean deletePath,
                         HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        ClassWizardState state = stateOf(session);
        if (state != null && deletePath && state.getLearningPathId() != null) {
            try {
                pathService.delete(state.getLearningPathId(), user);
            } catch (Exception e) {
                ra.addFlashAttribute("error", e.getMessage());
            }
        }
        session.removeAttribute(SESSION_KEY);
        return "redirect:/lecturer/classes";
    }

    // ===== HELPERS =====

    /** Lỗi chặn tạo lớp: lộ trình rỗng, node test chưa gắn rule, bài test chưa publish/không sở hữu. */
    private List<String> computeErrors(List<LearningNode> nodes, User user) {
        List<String> errors = new ArrayList<>();
        if (nodes.isEmpty()) {
            errors.add("Lộ trình chưa có node nào.");
            return errors;
        }

        Map<Integer, Assessment> owned = new HashMap<>();
        for (Assessment a : assessmentService.findByCreator(user)) {
            owned.put(a.getId(), a);
        }

        for (LearningNode n : nodes) {
            if (!"BRANCH_TEST".equals(n.getNodeType())) continue;
            BranchRule rule = branchRuleService.findByNodeId(n.getId()).orElse(null);
            if (rule == null || rule.getAssessmentId() == null) {
                errors.add("Node \"" + n.getTitle() + "\" chưa gắn bài test.");
                continue;
            }
            Assessment a = owned.get(rule.getAssessmentId());
            if (a == null) {
                errors.add("Bài test của node \"" + n.getTitle() + "\" không hợp lệ.");
            } else if (!"PUBLISHED".equals(a.getStatus())) {
                errors.add("Bài test \"" + a.getTitle() + "\" chưa được publish.");
            }
        }
        return errors;
    }

    /** Cảnh báo không bắt buộc: nhánh trống, bài học chưa có nội dung. */
    private List<String> computeWarnings(Integer pathId, List<LearningNode> nodes, User user) {
        List<String> warnings = new ArrayList<>();
        Map<Integer, Integer> contentCounts = pathService.contentCounts(pathId);

        for (LearningNode n : nodes) {
            if ("BRANCH_TEST".equals(n.getNodeType())) {
                boolean hasPass = nodes.stream().anyMatch(x -> "PASS".equals(x.getBranchTag())
                        && x.getBranchOwnerNode() != null && x.getBranchOwnerNode().getId().equals(n.getId()));
                boolean hasFail = nodes.stream().anyMatch(x -> "FAIL".equals(x.getBranchTag())
                        && x.getBranchOwnerNode() != null && x.getBranchOwnerNode().getId().equals(n.getId()));
                if (!hasPass) warnings.add("Bài test \"" + n.getTitle() + "\" chưa có bài học cho nhánh ĐẠT.");
                if (!hasFail) warnings.add("Bài test \"" + n.getTitle() + "\" chưa có bài học cho nhánh CHƯA ĐẠT.");
            } else {
                Integer cnt = contentCounts.get(n.getId());
                if (cnt == null || cnt == 0) {
                    warnings.add("Bài học \"" + n.getTitle() + "\" chưa có nội dung.");
                }
            }
        }
        return warnings;
    }

    private ClassWizardState stateOf(HttpSession session) {
        Object o = session.getAttribute(SESSION_KEY);
        return (o instanceof ClassWizardState s) ? s : null;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
