package com.edunac.mentora.service.branching;

import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.branching.LearningNodeType;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.repository.branching.BranchRuleRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchRuleService {

    private final BranchRuleRepository branchRuleRepository;
    private final LearningNodeRepository learningNodeRepository;

    @Transactional
    public BranchRule saveRule(Integer nodeId, Integer assessmentId, Integer minScore) {
        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node không tồn tại"));

        if (!LearningNodeType.BRANCH_TEST.name().equals(node.getNodeType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chỉ node loại TEST mới được gắn Rule");
        }

        if (minScore == null || minScore < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Điểm thấp nhất phải >= 0");
        }

        BranchRule rule = branchRuleRepository.findByNodeId(nodeId)
                .orElseGet(BranchRule::new);

        rule.setNode(node);
        rule.setAssessmentId(assessmentId);
        rule.setMinScore(minScore);

        return branchRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public Optional<BranchRule> findByNodeId(Integer nodeId) {
        return branchRuleRepository.findByNodeId(nodeId);
    }

    @Transactional
    public void deleteByNodeId(Integer nodeId) {
        branchRuleRepository.findByNodeId(nodeId)
                .ifPresent(branchRuleRepository::delete);
    }
}