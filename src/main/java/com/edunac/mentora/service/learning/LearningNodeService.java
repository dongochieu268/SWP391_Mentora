package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningNodeService {

    private final LearningNodeRepository learningNodeRepository;

    public LearningNodeService(LearningNodeRepository learningNodeRepository) {
        this.learningNodeRepository = learningNodeRepository;
    }

    public LearningNode findById(Integer id) {
        return learningNodeRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy node học tập"));
    }

    public LearningNode findByIdForOwner(Integer nodeId, User requester) {
        LearningNode node = findById(nodeId);
        if (!node.getLearningPath().getCreatedBy().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập node này.");
        }
        return node;
    }
}
