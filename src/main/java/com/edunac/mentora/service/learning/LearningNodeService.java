package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.learning.LearningNode;
import com.edunac.mentora.repository.learning.LearningNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LearningNodeService {

    private final LearningNodeRepository learningNodeRepository;

    public LearningNodeService(LearningNodeRepository learningNodeRepository) {
        this.learningNodeRepository = learningNodeRepository;
    }

    public List<LearningNode> getAllNodes() {
        return learningNodeRepository.findAllWithPathOrdered();
    }

    public LearningNode findById(Integer id) {
        return learningNodeRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy node học tập"));
    }
}
