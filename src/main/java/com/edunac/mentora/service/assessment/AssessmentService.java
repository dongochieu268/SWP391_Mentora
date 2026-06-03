package com.edunac.mentora.service.assessment;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.*;
import com.edunac.mentora.dto.AssessmentForm;
import com.edunac.mentora.dto.QuestionForm;
import com.edunac.mentora.repository.assessment.AssessmentRepository;
import com.edunac.mentora.repository.assessment.QuestionOptionRepository;
import com.edunac.mentora.repository.assessment.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Transactional(readOnly = true)
    public List<Assessment> findByCreator(User creator) {
        return assessmentRepository.findByCreatedByIdOrderByCreatedAtDesc(creator.getId());
    }

    @Transactional(readOnly = true)
    public List<Assessment> findPublishedByCreator(User creator) {
        return assessmentRepository.findByCreatedByIdAndStatusOrderByCreatedAtDesc(
                creator.getId(), AssessmentStatus.PUBLISHED.name());
    }

    @Transactional(readOnly = true)
    public Assessment findById(Integer id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài test."));
    }

    @Transactional(readOnly = true)
    public Assessment findByIdAndOwner(Integer id, User requester) {
        Assessment assessment = findById(id);
        if (!assessment.getCreatedBy().getId().equals(requester.getId())) {
            throw new IllegalStateException("Bạn không có quyền truy cập bài test này.");
        }
        return assessment;
    }

    public Assessment create(AssessmentForm form, User creator) {
        validateAssessmentForm(form);

        Assessment assessment = new Assessment();
        applyAssessmentForm(assessment, form);
        assessment.setStatus(AssessmentStatus.DRAFT.name());
        assessment.setCreatedBy(creator);
        return assessmentRepository.save(assessment);
    }

    public Assessment update(Integer assessmentId, AssessmentForm form, User requester) {
        Assessment assessment = findByIdAndOwner(assessmentId, requester);
        requireDraft(assessment);
        validateAssessmentForm(form);
        applyAssessmentForm(assessment, form);
        return assessmentRepository.save(assessment);
    }

    public Question addQuestion(Integer assessmentId, QuestionForm form, User requester) {
        Assessment assessment = findByIdAndOwner(assessmentId, requester);
        requireDraft(assessment);
        validateQuestionForm(form);

        Question question = new Question();
        question.setAssessment(assessment);
        question.setContent(form.getContent().trim());
        question.setDifficulty(normalizeEnum(form.getDifficulty(), QuestionDifficulty.class, "Mức độ câu hỏi"));
        question.setQuestionType(normalizeEnum(form.getQuestionType(), QuestionType.class, "Loại câu hỏi"));
        question.setScore(form.getScore());
        Question savedQuestion = questionRepository.save(question);

        optionRepository.saveAll(buildOptions(savedQuestion, form));
        return savedQuestion;
    }

    public void deleteQuestion(Integer assessmentId, Integer questionId, User requester) {
        Assessment assessment = findByIdAndOwner(assessmentId, requester);
        requireDraft(assessment);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi."));
        if (!question.getAssessment().getId().equals(assessmentId)) {
            throw new IllegalArgumentException("Câu hỏi không thuộc bài test này.");
        }

        optionRepository.deleteByQuestion_Id(questionId);
        questionRepository.delete(question);
    }

    public Assessment publish(Integer assessmentId, User requester) {
        Assessment assessment = findByIdAndOwner(assessmentId, requester);
        requireDraft(assessment);
        validatePublishable(assessment);
        assessment.setStatus(AssessmentStatus.PUBLISHED.name());
        return assessmentRepository.save(assessment);
    }

    public Assessment cloneAssessment(Integer assessmentId, User requester) {
        Assessment original = findByIdAndOwner(assessmentId, requester);

        Assessment clone = new Assessment();
        clone.setTitle(cloneTitle(original.getTitle()));
        clone.setDescription(original.getDescription());
        clone.setType(original.getType());
        clone.setDeliveryMode(original.getDeliveryMode());
        clone.setDurationMinutes(original.getDurationMinutes());
        clone.setTotalScore(original.getTotalScore());
        clone.setStatus(AssessmentStatus.DRAFT.name());
        clone.setCreatedBy(requester);
        Assessment savedClone = assessmentRepository.save(clone);

        List<Question> questions = questionRepository.findByAssessment_IdOrderByIdAsc(original.getId());
        for (Question question : questions) {
            Question clonedQuestion = new Question();
            clonedQuestion.setAssessment(savedClone);
            clonedQuestion.setContent(question.getContent());
            clonedQuestion.setDifficulty(question.getDifficulty());
            clonedQuestion.setQuestionType(question.getQuestionType());
            clonedQuestion.setScore(question.getScore());
            Question savedQuestion = questionRepository.save(clonedQuestion);

            List<QuestionOption> clonedOptions = new ArrayList<>();
            for (QuestionOption option : optionRepository.findByQuestion_IdOrderByIdAsc(question.getId())) {
                QuestionOption clonedOption = new QuestionOption();
                clonedOption.setQuestion(savedQuestion);
                clonedOption.setContent(option.getContent());
                clonedOption.setCorrect(option.isCorrect());
                clonedOptions.add(clonedOption);
            }
            optionRepository.saveAll(clonedOptions);
        }

        return savedClone;
    }

    @Transactional(readOnly = true)
    public List<Question> findQuestions(Integer assessmentId) {
        return questionRepository.findByAssessment_IdOrderByIdAsc(assessmentId);
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<QuestionOption>> findOptionsGroupedByQuestion(List<Question> questions) {
        Map<Integer, List<QuestionOption>> result = new LinkedHashMap<>();
        if (questions == null || questions.isEmpty()) {
            return result;
        }

        List<Integer> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        for (Question question : questions) {
            result.put(question.getId(), new ArrayList<>());
        }
        for (QuestionOption option : optionRepository.findByQuestion_IdInOrderByQuestion_IdAscIdAsc(questionIds)) {
            result.computeIfAbsent(option.getQuestion().getId(), ignored -> new ArrayList<>()).add(option);
        }
        return result;
    }

    public AssessmentForm toForm(Assessment assessment) {
        AssessmentForm form = new AssessmentForm();
        form.setId(assessment.getId());
        form.setTitle(assessment.getTitle());
        form.setDescription(assessment.getDescription());
        form.setType(assessment.getType());
        form.setDeliveryMode(assessment.getDeliveryMode());
        form.setDurationMinutes(assessment.getDurationMinutes());
        form.setTotalScore(assessment.getTotalScore());
        return form;
    }

    private void applyAssessmentForm(Assessment assessment, AssessmentForm form) {
        assessment.setTitle(form.getTitle().trim());
        assessment.setDescription(blankToNull(form.getDescription()));
        assessment.setType(normalizeEnum(form.getType(), AssessmentType.class, "Loại bài test"));
        assessment.setDeliveryMode(normalizeEnum(form.getDeliveryMode(), DeliveryMode.class, "Hình thức làm bài"));
        assessment.setDurationMinutes(form.getDurationMinutes());
        assessment.setTotalScore(form.getTotalScore());
    }

    private void validateAssessmentForm(AssessmentForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu bài test không hợp lệ.");
        }
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            throw new IllegalArgumentException("Tên bài test không được để trống.");
        }
        if (form.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException("Tên bài test tối đa 200 ký tự.");
        }
        if (form.getDurationMinutes() == null || form.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Thời lượng phải lớn hơn 0 phút.");
        }
        if (form.getTotalScore() == null || form.getTotalScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng điểm phải lớn hơn 0.");
        }
    }

    private void validateQuestionForm(QuestionForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu câu hỏi không hợp lệ.");
        }
        if (form.getContent() == null || form.getContent().isBlank()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống.");
        }
        if (form.getScore() == null || form.getScore().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Điểm câu hỏi phải lớn hơn 0.");
        }
        normalizeEnum(form.getDifficulty(), QuestionDifficulty.class, "Mức độ câu hỏi");
        normalizeEnum(form.getQuestionType(), QuestionType.class, "Loại câu hỏi");

        List<String> optionContents = form.getOptionContents();
        Integer correctIndex = form.getCorrectOptionIndex();
        if (optionContents == null || optionContents.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập đáp án.");
        }
        if (correctIndex == null || correctIndex < 0 || correctIndex >= optionContents.size()) {
            throw new IllegalArgumentException("Vui lòng chọn đáp án đúng.");
        }
        if (optionContents.get(correctIndex) == null || optionContents.get(correctIndex).isBlank()) {
            throw new IllegalArgumentException("Đáp án đúng không được để trống.");
        }

        long filledOptionCount = optionContents.stream()
                .filter(value -> value != null && !value.isBlank())
                .count();
        if (filledOptionCount < 2) {
            throw new IllegalArgumentException("Mỗi câu hỏi cần ít nhất 2 đáp án.");
        }
    }

    private List<QuestionOption> buildOptions(Question question, QuestionForm form) {
        List<QuestionOption> options = new ArrayList<>();
        List<String> optionContents = form.getOptionContents();
        for (int i = 0; i < optionContents.size(); i++) {
            String content = optionContents.get(i);
            if (content == null || content.isBlank()) {
                continue;
            }

            QuestionOption option = new QuestionOption();
            option.setQuestion(question);
            option.setContent(content.trim());
            option.setCorrect(i == form.getCorrectOptionIndex());
            options.add(option);
        }
        return options;
    }

    private void validatePublishable(Assessment assessment) {
        List<Question> questions = questionRepository.findByAssessment_IdOrderByIdAsc(assessment.getId());
        if (questions.isEmpty()) {
            throw new IllegalStateException("Cần ít nhất 1 câu hỏi trước khi publish bài test.");
        }

        BigDecimal questionTotal = BigDecimal.ZERO;
        for (Question question : questions) {
            long optionCount = optionRepository.countByQuestion_Id(question.getId());
            long correctCount = optionRepository.countByQuestion_IdAndCorrectTrue(question.getId());
            if (optionCount < 2 || correctCount != 1) {
                throw new IllegalStateException("Mỗi câu hỏi phải có ít nhất 2 đáp án và đúng 1 đáp án đúng.");
            }
            questionTotal = questionTotal.add(question.getScore());
        }

        if (questionTotal.compareTo(assessment.getTotalScore()) != 0) {
            throw new IllegalStateException("Tổng điểm các câu hỏi phải bằng tổng điểm bài test.");
        }
    }

    private void requireDraft(Assessment assessment) {
        if (!AssessmentStatus.DRAFT.name().equals(assessment.getStatus())) {
            throw new IllegalStateException("Chỉ bài test ở trạng thái DRAFT mới được chỉnh sửa.");
        }
    }

    private <E extends Enum<E>> String normalizeEnum(String value, Class<E> enumType, String label) {
        if (value == null || value.isBlank()) {
            if (enumType == AssessmentType.class) return AssessmentType.QUIZ.name();
            if (enumType == DeliveryMode.class) return DeliveryMode.SELF_PACED.name();
            if (enumType == QuestionType.class) return QuestionType.MULTIPLE_CHOICE.name();
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
        String normalized = value.trim().toUpperCase();
        try {
            Enum.valueOf(enumType, normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(label + " không hợp lệ.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String cloneTitle(String originalTitle) {
        String suffix = " (Clone)";
        String baseTitle = originalTitle == null || originalTitle.isBlank()
                ? "Untitled assessment"
                : originalTitle.trim();
        int maxBaseLength = 200 - suffix.length();
        if (baseTitle.length() > maxBaseLength) {
            baseTitle = baseTitle.substring(0, maxBaseLength).trim();
        }
        return baseTitle + suffix;
    }
}
