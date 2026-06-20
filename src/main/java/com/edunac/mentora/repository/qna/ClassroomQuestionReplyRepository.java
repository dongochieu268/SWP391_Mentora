package com.edunac.mentora.repository.qna;

import com.edunac.mentora.domain.qna.ClassroomQuestionReply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomQuestionReplyRepository extends JpaRepository<ClassroomQuestionReply, Integer> {

    boolean existsByQuestionIdAndOfficialTrue(Integer questionId);

    void deleteByQuestionId(Integer questionId);
}
