package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NodeContentRepository extends JpaRepository<NodeContent, Integer> {

    List<NodeContent> findByNode_IdOrderByDisplayOrderAscIdAsc(Integer nodeId);

    Optional<NodeContent> findByIdAndNode_Id(Integer id, Integer nodeId);
}
