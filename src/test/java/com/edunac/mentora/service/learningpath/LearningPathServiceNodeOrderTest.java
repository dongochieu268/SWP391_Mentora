package com.edunac.mentora.service.learningpath;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.domain.learningpath.LearningPath;
import com.edunac.mentora.dto.LearningNodeForm;
import com.edunac.mentora.repository.branching.BranchRuleRepository;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import com.edunac.mentora.repository.learningpath.LearningPathRepository;
import com.edunac.mentora.repository.subject.SubjectRepository;
import com.edunac.mentora.service.learning.NodeContentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPathServiceNodeOrderTest {

    private static final int PATH_ID = 17;

    private final Map<Integer, LearningNode> nodesById = new LinkedHashMap<>();
    private final Map<Integer, BigDecimal> persistedOrders = new LinkedHashMap<>();

    private LearningNodeRepository nodeRepository;
    private LearningPathService service;
    private LearningPath path;
    private User owner;
    private int nextNodeId;

    @BeforeEach
    void setUp() {
        LearningPathRepository pathRepository = mock(LearningPathRepository.class);
        nodeRepository = mock(LearningNodeRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        NodeContentRepository nodeContentRepository = mock(NodeContentRepository.class);
        NodeContentStorageService storageService = mock(NodeContentStorageService.class);
        NodeProgressRepository nodeProgressRepository = mock(NodeProgressRepository.class);
        ClassroomNodeStatusRepository classroomNodeStatusRepository = mock(ClassroomNodeStatusRepository.class);
        ClassroomRepository classroomRepository = mock(ClassroomRepository.class);
        BranchRuleRepository branchRuleRepository = mock(BranchRuleRepository.class);

        owner = new User();
        owner.setId(7);
        path = new LearningPath();
        path.setId(PATH_ID);
        path.setCreatedBy(owner);

        when(pathRepository.findById(PATH_ID)).thenReturn(Optional.of(path));
        when(nodeRepository.findByLearningPathIdOrderByNodeOrderAsc(PATH_ID))
                .thenAnswer(ignored -> sortedNodes());
        doAnswer(invocation -> persistOne(invocation.getArgument(0)))
                .when(nodeRepository).save(any(LearningNode.class));
        doAnswer(invocation -> persistSequentially(invocation.getArgument(0)))
                .when(nodeRepository).saveAll(anyList());
        doAnswer(invocation -> persistSequentially(invocation.getArgument(0)))
                .when(nodeRepository).saveAllAndFlush(anyList());

        service = new LearningPathService(
                pathRepository,
                nodeRepository,
                subjectRepository,
                nodeContentRepository,
                storageService,
                nodeProgressRepository,
                classroomNodeStatusRepository,
                classroomRepository,
                branchRuleRepository
        );
    }

    @Test
    void moveNodeUsesCollisionFreeOrders() {
        registerNode(1, "1");
        registerNode(2, "2");
        registerNode(3, "3");
        registerNode(4, "4");

        assertDoesNotThrow(() -> service.moveNode(PATH_ID, 4, "up", owner));

        assertEquals(List.of(1, 2, 4, 3), sortedNodes().stream().map(LearningNode::getId).toList());
        assertEquals(new BigDecimal("3"), nodesById.get(4).getNodeOrder());
        assertEquals(new BigDecimal("4"), nodesById.get(3).getNodeOrder());
        assertPersistedOrdersAreUnique();
    }

    @Test
    void addNodeNormalizesWithCollisionFreeOrders() {
        registerNode(1, "1");
        registerNode(2, "2");
        registerNode(3, "2.0005");
        registerNode(4, "3");
        registerNode(5, "4");
        nextNodeId = 6;

        LearningNodeForm form = new LearningNodeForm();
        form.setTitle("Inserted node");
        form.setAfterNodeId(2);

        LearningNode saved = assertDoesNotThrow(() -> service.addNode(PATH_ID, form, owner));

        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("3"), saved.getNodeOrder());
        assertEquals(
                List.of("1", "2", "3", "4", "5", "6"),
                sortedNodes().stream().map(node -> node.getNodeOrder().toPlainString()).toList()
        );
        assertPersistedOrdersAreUnique();
    }

    private void registerNode(int id, String order) {
        LearningNode node = new LearningNode();
        node.setId(id);
        node.setLearningPath(path);
        node.setTitle("Node " + id);
        node.setNodeOrder(new BigDecimal(order));
        node.setNodeType("LESSON");
        node.setBranchTag("MAIN");
        nodesById.put(id, node);
        persistedOrders.put(id, node.getNodeOrder());
        nextNodeId = Math.max(nextNodeId, id + 1);
    }

    private LearningNode persistOne(LearningNode node) {
        if (node.getId() == null) {
            node.setId(nextNodeId++);
        }
        nodesById.put(node.getId(), node);
        persistSequentially(List.of(node));
        return node;
    }

    private <S extends LearningNode> List<S> persistSequentially(Iterable<S> submitted) {
        List<S> saved = new ArrayList<>(StreamSupport.stream(submitted.spliterator(), false).toList());
        for (S node : saved) {
            boolean duplicate = persistedOrders.entrySet().stream()
                    .anyMatch(entry -> !entry.getKey().equals(node.getId())
                            && entry.getValue().compareTo(node.getNodeOrder()) == 0);
            if (duplicate) {
                throw new DataIntegrityViolationException("UQ_node_order");
            }
            persistedOrders.put(node.getId(), node.getNodeOrder());
        }
        return saved;
    }

    private List<LearningNode> sortedNodes() {
        return nodesById.values().stream()
                .sorted(Comparator.comparing(LearningNode::getNodeOrder))
                .toList();
    }

    private void assertPersistedOrdersAreUnique() {
        long uniqueCount = persistedOrders.values().stream()
                .map(BigDecimal::stripTrailingZeros)
                .distinct()
                .count();
        assertTrue(uniqueCount == persistedOrders.size(), "Persisted node orders must be unique");
    }
}
