package com.edunac.mentora.repository.learningpath;

import com.edunac.mentora.domain.learningpath.LearningNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningNodeRepository extends JpaRepository<LearningNode, Integer> {

    List<LearningNode> findByLearningPathIdOrderByNodeOrderAsc(Integer learningPathId);
}
