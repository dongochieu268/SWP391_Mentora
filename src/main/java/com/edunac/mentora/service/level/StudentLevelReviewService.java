package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.level.*;
import com.edunac.mentora.repository.UserRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import com.edunac.mentora.repository.level.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class StudentLevelReviewService {
    private final StudentLevelReviewRepository repository;
    private final NodeLevelRepository levelRepository;
    private final NodeLevelAttemptRepository attemptRepository;
    private final LevelMaterialRepository levelMaterialRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;

    public boolean isReviewed(Integer levelId,Integer studentId,Integer classroomId) {
        return repository.existsByNodeLevel_IdAndStudent_IdAndClassroom_Id(levelId,studentId,classroomId);
    }
    public boolean requiresReview(NodeLevel level) {
        return !levelMaterialRepository.findByNodeLevel_Id(level.getId()).isEmpty();
    }
    @Transactional
    public void markReviewed(Integer levelId,Integer nodeId,Integer studentId,Integer classroomId) {
        NodeLevel level=levelRepository.findById(levelId).orElseThrow(()->new EntityNotFoundException("Không tìm thấy level."));
        if(!level.getLearningNode().getId().equals(nodeId)) throw new IllegalArgumentException("Level không thuộc bài học này.");
        if(!requiresReview(level)) throw new IllegalStateException("Level này không có material cần xác nhận.");
        levelRepository.findTopByLearningNode_IdAndLevelNumberLessThanOrderByLevelNumberDesc(nodeId,level.getLevelNumber())
                .ifPresent(previous -> {
                    if(!attemptRepository.existsByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusAndPassedTrue(
                            previous.getId(),studentId,classroomId,"SUBMITTED")) {
                        throw new IllegalStateException("Bạn cần đạt level trước để mở nội dung học.");
                    }
                });
        if(isReviewed(levelId,studentId,classroomId)) return;
        StudentLevelReview review=new StudentLevelReview();
        review.setNodeLevel(level); review.setStudent(userRepository.getReferenceById(studentId));
        review.setClassroom(classroomRepository.getReferenceById(classroomId)); repository.save(review);
    }
}
