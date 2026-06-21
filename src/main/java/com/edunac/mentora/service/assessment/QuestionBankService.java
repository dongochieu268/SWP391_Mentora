package com.edunac.mentora.service.assessment;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.*;
import com.edunac.mentora.domain.subject.Subject;
import com.edunac.mentora.dto.QuestionBankForm;
import com.edunac.mentora.repository.assessment.*;
import com.edunac.mentora.repository.subject.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionBankService {

    private final QuestionBankRepository bankRepository;
    private final BankQuestionRepository bankQuestionRepository;
    private final BankQuestionOptionRepository bankOptionRepository;
    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final SubjectRepository subjectRepository;

    public QuestionBankService(QuestionBankRepository bankRepository,
                               BankQuestionRepository bankQuestionRepository,
                               BankQuestionOptionRepository bankOptionRepository,
                               AssessmentRepository assessmentRepository,
                               QuestionRepository questionRepository,
                               QuestionOptionRepository optionRepository,
                               SubjectRepository subjectRepository) {
        this.bankRepository = bankRepository;
        this.bankQuestionRepository = bankQuestionRepository;
        this.bankOptionRepository = bankOptionRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<BankQuestion> findActiveBySubject(Integer subjectId, String keyword, String difficulty, String type) {
        if (subjectId == null) return List.of();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return bankQuestionRepository
                .findByQuestionBank_Subject_IdAndStatusOrderByUpdatedAtDesc(subjectId, BankQuestionStatus.ACTIVE.name())
                .stream()
                .filter(q -> normalizedKeyword.isEmpty() || q.getContent().toLowerCase().contains(normalizedKeyword))
                .filter(q -> difficulty == null || difficulty.isBlank() || q.getDifficulty().equalsIgnoreCase(difficulty))
                .filter(q -> type == null || type.isBlank() || q.getQuestionType().equalsIgnoreCase(type))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<BankQuestionOption>> findOptionsGrouped(List<BankQuestion> questions) {
        Map<Integer, List<BankQuestionOption>> result = new LinkedHashMap<>();
        if (questions == null || questions.isEmpty()) return result;
        List<Integer> ids = questions.stream().map(BankQuestion::getId).toList();
        ids.forEach(id -> result.put(id, new ArrayList<>()));
        for (BankQuestionOption option :
                bankOptionRepository.findByBankQuestion_IdInOrderByBankQuestion_IdAscDisplayOrderAscIdAsc(ids)) {
            result.computeIfAbsent(option.getBankQuestion().getId(), ignored -> new ArrayList<>()).add(option);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Page<BankQuestion> searchAvailableForAssessment(Integer assessmentId, String keyword,
                                                           String difficulty,
                                                           int page, int size, User requester) {
        Assessment assessment = requireEditableAssessment(assessmentId, requester);
        String normalizedDifficulty = normalizeOptionalEnum(difficulty, QuestionDifficulty.class);
        return bankQuestionRepository.searchAvailableForAssessment(
                assessmentId,
                assessment.getSubject().getId(),
                BankQuestionStatus.ACTIVE.name(),
                keyword == null ? "" : keyword.trim(),
                normalizedDifficulty,
                PageRequest.of(Math.max(page, 0), Math.max(5, Math.min(size, 30)))
        );
    }

    @Transactional(readOnly = true)
    public List<BankQuestion> previewRandom(Integer assessmentId, Integer count, String difficulty, User requester) {
        Assessment assessment = requireEditableAssessment(assessmentId, requester);
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("Số câu random phải lớn hơn 0.");
        }

        Set<Integer> existingSourceIds = questionRepository.findByAssessment_IdOrderByIdAsc(assessmentId).stream()
                .map(Question::getSourceBankQuestion)
                .filter(Objects::nonNull)
                .map(BankQuestion::getId)
                .collect(Collectors.toSet());
        List<BankQuestion> eligible = new ArrayList<>(findActiveBySubject(
                assessment.getSubject().getId(), null, difficulty, null));
        eligible.removeIf(question -> existingSourceIds.contains(question.getId()));
        if (eligible.size() < count) {
            throw new IllegalStateException("Ngân hàng chỉ còn " + eligible.size()
                    + " câu phù hợp chưa có trong bài test.");
        }

        Collections.shuffle(eligible);
        return eligible.subList(0, count);
    }

    public BankQuestion create(QuestionBankForm form, User creator) {
        validateForm(form);
        Subject subject = requireActiveSubject(form.getSubjectId());
        QuestionBank bank = bankRepository.findBySubject_Id(subject.getId()).orElseGet(() -> {
            QuestionBank created = new QuestionBank();
            created.setSubject(subject);
            return bankRepository.save(created);
        });

        BankQuestion question = new BankQuestion();
        question.setQuestionBank(bank);
        question.setCreatedBy(creator);
        applyForm(question, form);
        BankQuestion saved = bankQuestionRepository.save(question);
        bankOptionRepository.saveAll(buildBankOptions(saved, form));
        return saved;
    }

    public BankQuestion update(Integer id, QuestionBankForm form, User requester) {
        validateForm(form);
        BankQuestion question = requireOwnedQuestion(id, requester);
        if (!question.getQuestionBank().getSubject().getId().equals(form.getSubjectId())) {
            throw new IllegalArgumentException("Không thể chuyển câu hỏi sang môn học khác.");
        }
        applyForm(question, form);
        bankOptionRepository.deleteByBankQuestion_Id(id);
        bankOptionRepository.saveAll(buildBankOptions(question, form));
        return bankQuestionRepository.save(question);
    }

    public void archive(Integer id, User requester) {
        BankQuestion question = requireOwnedQuestion(id, requester);
        question.setStatus(BankQuestionStatus.ARCHIVED.name());
        bankQuestionRepository.save(question);
    }

    public int importSelected(Integer assessmentId, Collection<Integer> selectedIds, User requester) {
        Assessment assessment = requireEditableAssessment(assessmentId, requester);
        if (selectedIds == null || selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi.");
        }

        Set<Integer> uniqueIds = new LinkedHashSet<>(selectedIds);
        Map<Integer, BankQuestion> selectedById = bankQuestionRepository.findByIdIn(uniqueIds).stream()
                .collect(Collectors.toMap(BankQuestion::getId, question -> question));
        List<BankQuestion> selected = uniqueIds.stream()
                .map(selectedById::get)
                .filter(Objects::nonNull)
                .toList();
        validateImportQuestions(assessment, selected, uniqueIds.size());

        Set<Integer> existingSourceIds = questionRepository
                .findByAssessment_IdAndSourceBankQuestion_IdIn(assessmentId, uniqueIds)
                .stream()
                .map(q -> q.getSourceBankQuestion().getId())
                .collect(Collectors.toSet());

        int imported = 0;
        for (BankQuestion source : selected) {
            if (!existingSourceIds.contains(source.getId())) {
                copyToAssessment(source, assessment, QuestionSelectionMethod.MANUAL_IMPORT, false);
                imported++;
            }
        }
        if (imported == 0) throw new IllegalStateException("Các câu hỏi đã chọn đã có trong bài test.");
        return imported;
    }

    public int importRandomSelected(Integer assessmentId, Collection<Integer> selectedIds,
                                    boolean shuffleAnswers, User requester) {
        Assessment assessment = requireEditableAssessment(assessmentId, requester);
        if (selectedIds == null || selectedIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng random danh sách câu hỏi trước.");
        }

        Set<Integer> uniqueIds = new LinkedHashSet<>(selectedIds);
        Map<Integer, BankQuestion> selectedById = bankQuestionRepository.findByIdIn(uniqueIds).stream()
                .collect(Collectors.toMap(BankQuestion::getId, question -> question));
        List<BankQuestion> selected = uniqueIds.stream()
                .map(selectedById::get)
                .filter(Objects::nonNull)
                .toList();
        validateImportQuestions(assessment, selected, uniqueIds.size());
        if (!questionRepository.findByAssessment_IdAndSourceBankQuestion_IdIn(assessmentId, uniqueIds).isEmpty()) {
            throw new IllegalStateException("Danh sách random có câu hỏi đã được thêm. Vui lòng random lại.");
        }

        selected.forEach(source -> copyToAssessment(
                source, assessment, QuestionSelectionMethod.RANDOM_IMPORT, shuffleAnswers));
        return selected.size();
    }

    public int saveAssessmentQuestions(Integer assessmentId, Collection<Integer> questionIds, User requester) {
        Assessment assessment = requireEditableAssessment(assessmentId, requester);
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một câu hỏi tự soạn.");
        }
        QuestionBank bank = bankRepository.findBySubject_Id(assessment.getSubject().getId()).orElseGet(() -> {
            QuestionBank created = new QuestionBank();
            created.setSubject(assessment.getSubject());
            return bankRepository.save(created);
        });

        int savedCount = 0;
        for (Integer questionId : new LinkedHashSet<>(questionIds)) {
            Question source = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi trong bài test."));
            if (!source.getAssessment().getId().equals(assessmentId)) {
                throw new IllegalArgumentException("Câu hỏi không thuộc bài test này.");
            }
            if (source.getSourceBankQuestion() != null) continue;

            BankQuestion bankQuestion = new BankQuestion();
            bankQuestion.setQuestionBank(bank);
            bankQuestion.setCreatedBy(requester);
            bankQuestion.setContent(source.getContent());
            bankQuestion.setDifficulty(source.getDifficulty());
            bankQuestion.setQuestionType(source.getQuestionType());
            bankQuestion.setDefaultScore(source.getScore());
            BankQuestion saved = bankQuestionRepository.save(bankQuestion);

            List<BankQuestionOption> bankOptions = new ArrayList<>();
            for (QuestionOption option : optionRepository.findByQuestion_IdOrderByDisplayOrderAscIdAsc(source.getId())) {
                BankQuestionOption copy = new BankQuestionOption();
                copy.setBankQuestion(saved);
                copy.setContent(option.getContent());
                copy.setCorrect(option.isCorrect());
                copy.setDisplayOrder(option.getDisplayOrder());
                bankOptions.add(copy);
            }
            bankOptionRepository.saveAll(bankOptions);
            source.setSourceBankQuestion(saved);
            questionRepository.save(source);
            savedCount++;
        }
        if (savedCount == 0) throw new IllegalStateException("Các câu hỏi đã chọn đã có trong ngân hàng.");
        return savedCount;
    }

    private void copyToAssessment(BankQuestion source, Assessment assessment,
                                  QuestionSelectionMethod method, boolean shuffleAnswers) {
        Question copy = new Question();
        copy.setAssessment(assessment);
        copy.setContent(source.getContent());
        copy.setDifficulty(source.getDifficulty());
        copy.setQuestionType(source.getQuestionType());
        copy.setScore(source.getDefaultScore());
        copy.setSourceBankQuestion(source);
        copy.setSelectionMethod(method.name());
        Question saved = questionRepository.save(copy);

        List<BankQuestionOption> sourceOptions =
                new ArrayList<>(bankOptionRepository.findByBankQuestion_IdOrderByDisplayOrderAscIdAsc(source.getId()));
        if (shuffleAnswers) Collections.shuffle(sourceOptions);
        List<QuestionOption> copies = new ArrayList<>();
        for (int i = 0; i < sourceOptions.size(); i++) {
            BankQuestionOption sourceOption = sourceOptions.get(i);
            QuestionOption option = new QuestionOption();
            option.setQuestion(saved);
            option.setContent(sourceOption.getContent());
            option.setCorrect(sourceOption.isCorrect());
            option.setDisplayOrder(i);
            copies.add(option);
        }
        optionRepository.saveAll(copies);
    }

    private Assessment requireEditableAssessment(Integer id, User requester) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài test."));
        if (!assessment.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa bài test này.");
        }
        if (!AssessmentStatus.DRAFT.name().equals(assessment.getStatus())) {
            throw new IllegalStateException("Chỉ bài test DRAFT mới có thể thay đổi câu hỏi.");
        }
        if (assessment.getSubject() == null) {
            throw new IllegalStateException("Vui lòng chọn môn học cho bài test trước.");
        }
        return assessment;
    }

    private void validateImportQuestions(Assessment assessment, List<BankQuestion> selected, int requestedCount) {
        if (selected.size() != requestedCount) throw new IllegalArgumentException("Có câu hỏi không tồn tại.");
        for (BankQuestion question : selected) {
            if (!BankQuestionStatus.ACTIVE.name().equals(question.getStatus())) {
                throw new IllegalArgumentException("Không thể dùng câu hỏi đã lưu trữ.");
            }
            if (!question.getQuestionBank().getSubject().getId().equals(assessment.getSubject().getId())) {
                throw new IllegalArgumentException("Chỉ được chọn câu hỏi thuộc cùng môn với bài test.");
            }
        }
    }

    private BankQuestion requireOwnedQuestion(Integer id, User requester) {
        BankQuestion question = bankQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi."));
        if (!question.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Chỉ người tạo mới được sửa hoặc lưu trữ câu hỏi này.");
        }
        return question;
    }

    private Subject requireActiveSubject(Integer id) {
        if (id == null) throw new IllegalArgumentException("Vui lòng chọn môn học.");
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học."));
        if (!subject.isActive()) throw new IllegalArgumentException("Môn học không còn hoạt động.");
        return subject;
    }

    private void applyForm(BankQuestion question, QuestionBankForm form) {
        question.setContent(form.getContent().trim());
        question.setDifficulty(normalizeEnum(form.getDifficulty(), QuestionDifficulty.class, "Mức độ"));
        question.setQuestionType(normalizeEnum(form.getQuestionType(), QuestionType.class, "Loại câu hỏi"));
        question.setDefaultScore(form.getDefaultScore());
    }

    private List<BankQuestionOption> buildBankOptions(BankQuestion question, QuestionBankForm form) {
        List<BankQuestionOption> result = new ArrayList<>();
        int displayOrder = 0;
        for (int i = 0; i < form.getOptionContents().size(); i++) {
            String content = form.getOptionContents().get(i);
            if (content == null || content.isBlank()) continue;
            BankQuestionOption option = new BankQuestionOption();
            option.setBankQuestion(question);
            option.setContent(content.trim());
            option.setCorrect(i == form.getCorrectOptionIndex());
            option.setDisplayOrder(displayOrder++);
            result.add(option);
        }
        return result;
    }

    private void validateForm(QuestionBankForm form) {
        if (form == null || form.getContent() == null || form.getContent().isBlank()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (form.getDefaultScore() == null || form.getDefaultScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm mặc định phải lớn hơn 0.");
        }
        normalizeEnum(form.getDifficulty(), QuestionDifficulty.class, "Mức độ");
        normalizeEnum(form.getQuestionType(), QuestionType.class, "Loại câu hỏi");
        List<String> options = form.getOptionContents();
        if (options == null || options.stream().filter(v -> v != null && !v.isBlank()).count() < 2) {
            throw new IllegalArgumentException("Câu hỏi cần ít nhất 2 đáp án.");
        }
        Integer correct = form.getCorrectOptionIndex();
        if (correct == null || correct < 0 || correct >= options.size()
                || options.get(correct) == null || options.get(correct).isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn một đáp án đúng hợp lệ.");
        }
    }

    private <E extends Enum<E>> String normalizeEnum(String value, Class<E> type, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không hợp lệ.");
        String normalized = value.trim().toUpperCase();
        try {
            Enum.valueOf(type, normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private <E extends Enum<E>> String normalizeOptionalEnum(String value, Class<E> type) {
        if (value == null || value.isBlank()) return "";
        String normalized = value.trim().toUpperCase();
        try {
            Enum.valueOf(type, normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
