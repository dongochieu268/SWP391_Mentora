package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.assessment.BankQuestionOption;
import com.edunac.mentora.domain.assessment.BankQuestionStatus;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.Material;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.assessment.BankQuestionOptionRepository;
import com.edunac.mentora.repository.assessment.BankQuestionRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bốc đề (S3 §5): đủ câu -> lấy đúng questionCount; thiếu câu -> lấy hết
 * bấy nhiêu, KHÔNG mượn chéo material khác (OQ7). Cover cả trường hợp
 * material không có câu nào (0 available).
 */
class NodeLevelQuestionServiceTest {

    private BankQuestionRepository bankQuestionRepository;
    private BankQuestionOptionRepository bankQuestionOptionRepository;
    private AttemptQuestionRepository attemptQuestionRepository;
    private NodeLevelQuestionService service;

    private final List<AttemptQuestion> savedQuestions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bankQuestionRepository = mock(BankQuestionRepository.class);
        bankQuestionOptionRepository = mock(BankQuestionOptionRepository.class);
        attemptQuestionRepository = mock(AttemptQuestionRepository.class);

        service = new NodeLevelQuestionService(
                bankQuestionRepository, bankQuestionOptionRepository, attemptQuestionRepository);

        savedQuestions.clear();
        when(attemptQuestionRepository.saveAll(any())).thenAnswer(invocation -> {
            List<AttemptQuestion> arg = invocation.getArgument(0);
            savedQuestions.addAll(arg);
            return arg;
        });

        // Mỗi bank question không có option nào cần thiết cho test này ->
        // trả về danh sách rỗng, không ảnh hưởng tới số lượng câu được chọn.
        when(bankQuestionOptionRepository.findByBankQuestion_Id(anyInt())).thenReturn(List.of());
    }

    @Test
    void material_du_cau_thi_lay_dung_questionCount() {
        Material material = material(1);
        LevelMaterial levelMaterial = levelMaterial(material, 5);

        when(bankQuestionRepository.findByMaterial_IdAndStatus(1, BankQuestionStatus.ACTIVE.name()))
                .thenReturn(pool(1, 10)); // 10 câu có sẵn, chỉ cần lấy 5

        service.generateQuestionSnapshot(attempt(), List.of(levelMaterial));

        assertEquals(5, savedQuestions.size(), "material đủ câu phải lấy đúng questionCount");
        // displayOrder phải liên tục 1..N, không trùng/lỗ
        List<Integer> orders = savedQuestions.stream().map(AttemptQuestion::getDisplayOrder).sorted().toList();
        assertEquals(List.of(1, 2, 3, 4, 5), orders);
    }

    @Test
    void material_thieu_cau_thi_lay_het_bay_nhieu_khong_muon_cheo() {
        Material materialA = material(1);
        Material materialB = material(2);
        LevelMaterial levelMaterialA = levelMaterial(materialA, 5); // yêu cầu 5, chỉ có 2
        LevelMaterial levelMaterialB = levelMaterial(materialB, 3); // yêu cầu 3, có đủ 3

        when(bankQuestionRepository.findByMaterial_IdAndStatus(1, BankQuestionStatus.ACTIVE.name()))
                .thenReturn(pool(1, 2));
        when(bankQuestionRepository.findByMaterial_IdAndStatus(2, BankQuestionStatus.ACTIVE.name()))
                .thenReturn(pool(2, 3));

        service.generateQuestionSnapshot(attempt(), List.of(levelMaterialA, levelMaterialB));

        // Tổng = 2 (thiếu, lấy hết) + 3 (đủ) = 5, KHÔNG được "mượn" thêm 3 câu
        // còn thiếu của material A từ material B.
        assertEquals(5, savedQuestions.size());
    }

    @Test
    void material_khong_co_cau_nao_thi_khong_dong_gop_cau_hoi_nao() {
        Material material = material(1);
        LevelMaterial levelMaterial = levelMaterial(material, 4);

        when(bankQuestionRepository.findByMaterial_IdAndStatus(1, BankQuestionStatus.ACTIVE.name()))
                .thenReturn(List.of());

        service.generateQuestionSnapshot(attempt(), List.of(levelMaterial));

        assertTrue(savedQuestions.isEmpty());
    }

    @Test
    void khong_chon_trung_cau_hoi_trong_cung_mot_material() {
        Material material = material(1);
        LevelMaterial levelMaterial = levelMaterial(material, 4);

        when(bankQuestionRepository.findByMaterial_IdAndStatus(1, BankQuestionStatus.ACTIVE.name()))
                .thenReturn(pool(1, 4)); // đúng bằng questionCount

        service.generateQuestionSnapshot(attempt(), List.of(levelMaterial));

        Set<Integer> sourceIds = new HashSet<>();
        for (AttemptQuestion q : savedQuestions) {
            sourceIds.add(q.getSourceBankQuestion().getId());
        }
        assertEquals(4, sourceIds.size(), "không được chọn trùng cùng một bank question");
    }

    // ---- helpers ----

    private NodeLevelAttempt attempt() {
        return new NodeLevelAttempt();
    }

    private Material material(int id) {
        Material material = new Material();
        material.setId(id);
        return material;
    }

    private LevelMaterial levelMaterial(Material material, int questionCount) {
        LevelMaterial levelMaterial = new LevelMaterial();
        levelMaterial.setMaterial(material);
        levelMaterial.setQuestionCount(questionCount);
        return levelMaterial;
    }

    private List<BankQuestion> pool(int materialId, int count) {
        List<BankQuestion> pool = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BankQuestion q = new BankQuestion();
            q.setId(materialId * 1000 + i); // id duy nhất trên toàn bộ test
            q.setContent("Câu hỏi " + q.getId());
            q.setDifficulty("EASY");
            pool.add(q);
        }
        return pool;
    }
}