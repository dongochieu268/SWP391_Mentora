package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.Assessment;
import com.edunac.mentora.domain.assessment.Question;
import com.edunac.mentora.domain.assessment.QuestionDifficulty;
import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.service.assessment.AssessmentService;
import com.edunac.mentora.service.branching.BranchRuleService;
import com.edunac.mentora.service.learningpath.LearningPathService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Dựng model cho trang builder lộ trình (fragment builderBody) — dùng chung giữa
 * trang lộ trình standalone và bước 2 của wizard tạo lớp.
 */
@Component
public class PathBuilderViewSupport {

    private final LearningPathService pathService;
    private final AssessmentService assessmentService;
    private final BranchRuleService branchRuleService;

    public PathBuilderViewSupport(LearningPathService pathService,
                                  AssessmentService assessmentService,
                                  BranchRuleService branchRuleService) {
        this.pathService = pathService;
        this.assessmentService = assessmentService;
        this.branchRuleService = branchRuleService;
    }

    /** Dữ liệu sơ đồ: nhóm node theo luồng chính / nhánh PASS-FAIL, rule + đề test, số nội dung. */
    public void populateBuilderModel(Integer pathId, List<LearningNode> nodes, User user, Model model) {
        List<LearningNode> mainNodes = new ArrayList<>();
        Map<Integer, List<LearningNode>> passByOwner = new LinkedHashMap<>();
        Map<Integer, List<LearningNode>> failByOwner = new LinkedHashMap<>();
        List<LearningNode> orphanBranchNodes = new ArrayList<>();

        for (LearningNode n : nodes) {
            String t = n.getBranchTag();
            if ("PASS".equals(t) || "FAIL".equals(t)) {
                if (n.getBranchOwnerNode() == null) {
                    orphanBranchNodes.add(n);
                } else {
                    var map = "PASS".equals(t) ? passByOwner : failByOwner;
                    map.computeIfAbsent(n.getBranchOwnerNode().getId(), k -> new ArrayList<>()).add(n);
                }
            } else {
                mainNodes.add(n);
            }
        }

        Map<Integer, BranchRule> ruleByNode = new HashMap<>();
        for (LearningNode n : nodes) {
            if ("BRANCH_TEST".equals(n.getNodeType())) {
                branchRuleService.findByNodeId(n.getId()).ifPresent(r -> ruleByNode.put(n.getId(), r));
            }
        }

        Map<Integer, Assessment> assessmentById = new HashMap<>();
        for (Assessment a : assessmentService.findByCreator(user)) {
            assessmentById.put(a.getId(), a);
        }

        List<LearningNode> branchTestNodes = nodes.stream()
                .filter(n -> "BRANCH_TEST".equals(n.getNodeType()))
                .collect(Collectors.toList());

        // Điểm chèn "(+)" sau mỗi khối: node MAIN thường → chính nó; BRANCH_TEST → node nhánh cuối cùng của nó
        Map<Integer, Integer> blockLastId = new HashMap<>();
        for (LearningNode m : mainNodes) {
            blockLastId.put(m.getId(), m.getId());
        }
        for (LearningNode n : nodes) {
            if (n.getBranchOwnerNode() != null && blockLastId.containsKey(n.getBranchOwnerNode().getId())) {
                blockLastId.put(n.getBranchOwnerNode().getId(), n.getId());
            }
        }

        model.addAttribute("blockLastId", blockLastId);
        model.addAttribute("mainNodes", mainNodes);
        model.addAttribute("passByOwner", passByOwner);
        model.addAttribute("failByOwner", failByOwner);
        model.addAttribute("orphanBranchNodes", orphanBranchNodes);
        model.addAttribute("ruleByNode", ruleByNode);
        model.addAttribute("assessmentById", assessmentById);
        model.addAttribute("branchTestNodes", branchTestNodes);
        model.addAttribute("contentCounts", pathService.contentCounts(pathId));
        model.addAttribute("assessments", assessmentService.findPublishedByCreator(user));
    }

    /** Xử lý các query param mở modal node (addEnd/addAfter/editNode/addBranch+tag) và set default. */
    public void populateNodeModal(Integer addAfter, Boolean addEnd, Integer editNode,
                                  Integer addBranch, String tag,
                                  List<LearningNode> nodes, Model model) {
        if (!model.containsAttribute("nodeForm")) {
            if (editNode != null) {
                buildEditForm(editNode, nodes, model);
            } else if (addBranch != null && ("PASS".equals(tag) || "FAIL".equals(tag))) {
                buildAddBranchForm(addBranch, tag, nodes, model);
            } else if (addAfter != null || Boolean.TRUE.equals(addEnd)) {
                LearningNodeForm f = new LearningNodeForm();
                f.setAfterNodeId(addAfter);
                model.addAttribute("nodeForm", f);
                model.addAttribute("modalMode", "addMain");
                model.addAttribute("openNodeModal", true);
            }
        }

        if (!model.containsAttribute("nodeForm")) {
            model.addAttribute("nodeForm", new LearningNodeForm());
        }
        if (!model.containsAttribute("modalMode")) {
            model.addAttribute("modalMode", "none");
        }
        if (!model.containsAttribute("openNodeModal")) {
            model.addAttribute("openNodeModal", false);
        }
    }

    /** Panel soạn bài test bên phải builder: mở khi có query param testPanel={assessmentId}. */
    public void populateTestPanel(Integer assessmentId, User user, Model model) {
        if (assessmentId == null) return;
        try {
            Assessment a = assessmentService.findByIdAndOwner(assessmentId, user);
            List<Question> questions = assessmentService.findQuestions(a.getId());
            BigDecimal scoreSum = questions.stream()
                    .map(Question::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("panelAssessment", a);
            model.addAttribute("panelQuestions", questions);
            model.addAttribute("panelOptions", assessmentService.findOptionsGroupedByQuestion(questions));
            model.addAttribute("panelCanEdit", "DRAFT".equals(a.getStatus()));
            model.addAttribute("panelScoreSum", scoreSum);
            model.addAttribute("difficultyOptions", QuestionDifficulty.values());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
    }

    /** Form thêm node vào nhánh PASS/FAIL: branchTag + branchOwner điền sẵn, giảng viên chỉ nhập tiêu đề/mô tả. */
    private void buildAddBranchForm(Integer ownerNodeId, String tag, List<LearningNode> nodes, Model model) {
        for (LearningNode owner : nodes) {
            if (!owner.getId().equals(ownerNodeId) || !"BRANCH_TEST".equals(owner.getNodeType())) continue;

            LearningNodeForm f = new LearningNodeForm();
            f.setNodeType("LESSON");
            f.setBranchTag(tag);
            f.setBranchOwnerNodeId(ownerNodeId);
            // Chèn node mới vào cuối nhánh: sau node cuối cùng của cụm owner
            Integer afterId = ownerNodeId;
            for (LearningNode n : nodes) {
                if (n.getBranchOwnerNode() != null && n.getBranchOwnerNode().getId().equals(ownerNodeId)) {
                    afterId = n.getId();
                }
            }
            f.setAfterNodeId(afterId);

            model.addAttribute("nodeForm", f);
            model.addAttribute("modalMode", "addBranch");
            model.addAttribute("branchOwnerTitle", owner.getTitle());
            model.addAttribute("openNodeModal", true);
            return;
        }
    }

    private void buildEditForm(Integer editNodeId, List<LearningNode> nodes, Model model) {
        for (LearningNode n : nodes) {
            if (!n.getId().equals(editNodeId)) continue;

            LearningNodeForm f = new LearningNodeForm();
            f.setId(n.getId());
            f.setTitle(n.getTitle());
            f.setDescription(n.getDescription());
            if (n.getPrerequisite() != null) {
                f.setPrerequisiteNodeId(n.getPrerequisite().getId());
            }

            f.setNodeType(n.getNodeType() != null ? n.getNodeType() : "LESSON");
            f.setBranchTag(n.getBranchTag() != null ? n.getBranchTag() : "MAIN");
            if (n.getBranchOwnerNode() != null) {
                f.setBranchOwnerNodeId(n.getBranchOwnerNode().getId());
            }

            if ("BRANCH_TEST".equals(f.getNodeType())) {
                branchRuleService.findByNodeId(n.getId()).ifPresent(rule -> {
                    f.setAssessmentId(rule.getAssessmentId());
                    f.setMinScore(rule.getMinScore() != null ? rule.getMinScore().doubleValue() : null);
                });
            }

            model.addAttribute("nodeForm", f);
            model.addAttribute("modalMode", "edit");
            model.addAttribute("openNodeModal", true);
            return;
        }
    }
}
