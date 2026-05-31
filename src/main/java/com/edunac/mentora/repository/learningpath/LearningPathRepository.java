package com.edunac.mentora.repository.learningpath;

import com.edunac.mentora.domain.learningpath.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningPathRepository extends JpaRepository<LearningPath, Integer> {

    List<LearningPath> findByCreatedByIdOrderByCreatedAtDesc(Integer creatorId);

    List<LearningPath> findByCreatedByIdAndSubjectIdOrderByCreatedAtDesc(Integer creatorId, Integer subjectId);
}
