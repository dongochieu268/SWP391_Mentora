package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.level.NodeLevel;
import com.edunac.mentora.repository.level.NodeLevelAttemptRepository;
import com.edunac.mentora.repository.level.NodeLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeLevelProgressionPolicyTest {

    private NodeLevelAttemptRepository attemptRepository;
    private NodeLevelProgressionPolicy policy;
    private NodeLevel level1;
    private NodeLevel level2;

    @BeforeEach
    void setUp() {
        NodeLevelRepository levelRepository = mock(NodeLevelRepository.class);
        attemptRepository = mock(NodeLevelAttemptRepository.class);
        policy = new NodeLevelProgressionPolicy(levelRepository, attemptRepository);

        LearningNode node = new LearningNode();
        node.setId(10);
        level1 = level(101, 1, 2, node);
        level2 = level(102, 2, 2, node);
        when(levelRepository.findByLearningNode_IdOrderByLevelNumberAsc(10))
                .thenReturn(List.of(level1, level2));
    }

    @Test
    void failing_level_one_with_attempts_left_does_not_release_next_node() {
        when(attemptRepository.countByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatus(
                101, 7, 3, "SUBMITTED"))
                .thenReturn(1L);

        assertFalse(policy.canProgressBeyondNode(10, 7, 3));
    }

    @Test
    void exhausting_level_one_releases_next_node_without_passing_level_one() {
        when(attemptRepository.countByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatus(
                101, 7, 3, "SUBMITTED"))
                .thenReturn(2L);

        assertTrue(policy.canProgressBeyondNode(10, 7, 3));
    }

    @Test
    void granted_attempt_does_not_lock_an_already_released_next_node() {
        when(attemptRepository.countByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatus(
                101, 7, 3, "SUBMITTED"))
                .thenReturn(2L);
        assertTrue(policy.canProgressBeyondNode(10, 7, 3));
    }

    @Test
    void passing_level_one_releases_next_node_without_requiring_level_two() {
        when(attemptRepository
                .existsByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusAndPassedTrue(
                        101, 7, 3, "SUBMITTED"))
                .thenReturn(true);

        assertTrue(policy.canProgressBeyondNode(10, 7, 3));
    }

    private NodeLevel level(
            int id, int levelNumber, Integer maxAttempts, LearningNode node) {
        NodeLevel level = new NodeLevel();
        level.setId(id);
        level.setLevelNumber(levelNumber);
        level.setMaxAttempts(maxAttempts);
        level.setLearningNode(node);
        return level;
    }
}
