package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.learning.ContentType;
import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeContentForm;
import com.edunac.mentora.repository.learning.NodeContentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NodeContentService {

    private final NodeContentRepository nodeContentRepository;
    private final LearningNodeService learningNodeService;
    private final NodeContentStorageService storageService;

    public NodeContentService(
            NodeContentRepository nodeContentRepository,
            LearningNodeService learningNodeService,
            NodeContentStorageService storageService
    ) {
        this.nodeContentRepository = nodeContentRepository;
        this.learningNodeService = learningNodeService;
        this.storageService = storageService;
    }

    public List<NodeContent> getByNodeId(Integer nodeId) {
        learningNodeService.findById(nodeId);
        return nodeContentRepository.findByNode_IdOrderByDisplayOrderAscIdAsc(nodeId);
    }

    public NodeContent findByIdAndNodeId(Integer contentId, Integer nodeId) {
        return nodeContentRepository.findByIdAndNode_Id(contentId, nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nội dung"));
    }

    public NodeContentForm toForm(NodeContent content) {
        NodeContentForm form = new NodeContentForm();
        form.setId(content.getId());
        form.setNodeId(content.getNode().getId());
        form.setContentType(content.getContentType());
        form.setTitle(content.getTitle());
        form.setContentUrl(content.getContentUrl());
        form.setContentText(content.getContentText());
        form.setDisplayOrder(content.getDisplayOrder());
        return form;
    }

    public void saveForm(NodeContentForm form, MultipartFile mediaFile) {
        validateForm(form, mediaFile);
        LearningNode node = learningNodeService.findById(form.getNodeId());

        NodeContent content;
        String previousUrl = null;
        if (form.getId() == null) {
            content = new NodeContent();
            content.setNode(node);
        } else {
            content = findByIdAndNodeId(form.getId(), form.getNodeId());
            previousUrl = content.getContentUrl();
        }

        String newType = form.getContentType().trim().toUpperCase();
        content.setContentType(newType);
        content.setTitle(blankToNull(form.getTitle()));
        content.setDisplayOrder(form.getDisplayOrder() != null ? form.getDisplayOrder() : 1);

        if (ContentType.TEXT.name().equals(newType)) {
            content.setContentText(form.getContentText() != null ? form.getContentText().trim() : null);
            content.setContentUrl(null);
            storageService.deleteIfManaged(previousUrl);
        } else if (ContentType.LINK.name().equals(newType)) {
            content.setContentUrl(form.getContentUrl() != null ? form.getContentUrl().trim() : null);
            content.setContentText(null);
            storageService.deleteIfManaged(previousUrl);
        } else if (ContentType.VIDEO.name().equals(newType)) {
            content.setContentText(null);
            content.setContentUrl(resolveMediaUrl(mediaFile, ContentType.VIDEO, previousUrl, form.getId() == null));
        } else if (ContentType.FILE.name().equals(newType)) {
            content.setContentText(null);
            content.setContentUrl(resolveMediaUrl(mediaFile, ContentType.FILE, previousUrl, form.getId() == null));
        }

        nodeContentRepository.save(content);

        if (previousUrl != null && content.getContentUrl() != null && !previousUrl.equals(content.getContentUrl())) {
            storageService.deleteIfManaged(previousUrl);
        }
    }

    public void delete(Integer contentId, Integer nodeId) {
        NodeContent content = findByIdAndNodeId(contentId, nodeId);
        storageService.deleteIfManaged(content.getContentUrl());
        nodeContentRepository.delete(content);
    }

    private String resolveMediaUrl(
            MultipartFile mediaFile,
            ContentType type,
            String previousUrl,
            boolean isCreate
    ) {
        if (mediaFile != null && !mediaFile.isEmpty()) {
            return storageService.store(mediaFile, type);
        }
        if (previousUrl != null && !previousUrl.isBlank()) {
            return previousUrl;
        }
        if (isCreate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp để tải lên");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn tệp mới hoặc giữ tệp hiện tại");
    }

    private void validateForm(NodeContentForm form, MultipartFile mediaFile) {
        if (!ContentType.isValid(form.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Loại nội dung phải là TEXT, VIDEO, FILE hoặc LINK");
        }

        String type = form.getContentType().trim().toUpperCase();
        if (ContentType.TEXT.name().equals(type)) {
            if (form.getContentText() == null || form.getContentText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung văn bản không được để trống");
            }
        } else if (ContentType.LINK.name().equals(type)) {
            if (form.getContentUrl() == null || form.getContentUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết không được để trống");
            }
        } else if (ContentType.VIDEO.name().equals(type) || ContentType.FILE.name().equals(type)) {
            boolean hasFile = mediaFile != null && !mediaFile.isEmpty();
            boolean hasExisting = form.getId() != null;
            if (!hasFile && !hasExisting) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp để tải lên");
            }
        }

        if (form.getDisplayOrder() != null && form.getDisplayOrder() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thứ tự hiển thị phải >= 1");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
