package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Rules for progressing beyond a node independently from mastering all of
 * that node's levels. Passing any reachable level, or failing and exhausting
 * it, releases the next node. Higher levels remain optional and still require
 * the preceding level to be passed.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NodeLevelProgressionPolicy {

    private final NodeLevelRepository levelRepository;
    private final NodeLevelAttemptRepository attemptRepository;

    public boolean canProgressBeyondNode(
            Integer nodeId, Integer studentId, Integer classroomId) {

        List<NodeLevel> levels = levelRepository
                .findByLearningNode_IdOrderByLevelNumberAsc(nodeId);

        for (NodeLevel level : levels) {
            boolean passed = attemptRepository
                    .existsByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusAndPassedTrue(
                            level.getId(), studentId, classroomId,
                            AttemptStatus.SUBMITTED.name());

            if (passed) {
                return true;
            }

            // This is the first level the student has not passed, so higher
            // levels remain locked regardless of their own attempt settings.
            if (level.getMaxAttempts() == null) {
                return false;
            }

            long usedAttempts = attemptRepository
                    .countByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatus(
                            level.getId(), studentId, classroomId,
                            AttemptStatus.SUBMITTED.name());
            // Progression is one-way: once the original attempt allowance has
            // been exhausted, later grants are only opportunities to improve
            // the score and must never lock an already available next node.
            return usedAttempts >= level.getMaxAttempts();
        }

        return false;
    }
}
