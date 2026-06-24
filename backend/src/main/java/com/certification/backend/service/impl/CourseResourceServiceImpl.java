package com.certification.backend.service.impl;

import com.certification.backend.dto.request.CourseResourceRequest;
import com.certification.backend.dto.response.CourseResourceResponse;
import com.certification.backend.entity.CourseOffering;
import com.certification.backend.entity.CourseResource;
import com.certification.backend.entity.User;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.exception.BusinessException;
import com.certification.backend.repository.CourseOfferingRepository;
import com.certification.backend.repository.CourseResourceRepository;
import com.certification.backend.repository.UserRepository;
import com.certification.backend.service.CourseResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程资源管理业务实现
 */
@Service
@Transactional
public class CourseResourceServiceImpl implements CourseResourceService {

    private final CourseResourceRepository resourceRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CourseResourceServiceImpl(CourseResourceRepository resourceRepository,
                                     CourseOfferingRepository offeringRepository,
                                     UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.offeringRepository = offeringRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CourseResourceResponse add(CourseResourceRequest request, String username) {
        CourseOffering offering = getOfferingAndValidateOwner(request.getOfferingId(), username);

        CourseResource resource = new CourseResource();
        resource.setOfferingId(request.getOfferingId());
        resource.setFileName(request.getResourceName());
        resource.setFilePath(request.getResourceUrl());
        resource.setResourceType(request.getResourceType());

        CourseResource saved = resourceRepository.save(resource);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResourceResponse> listByOfferingId(Long offeringId, String username) {
        getOfferingAndValidateOwner(offeringId, username);
        List<CourseResource> resources = resourceRepository.findByOfferingId(offeringId);
        return resources.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        CourseResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程资源不存在"));

        // 校验该资源所属的开课记录是否属于当前教师
        getOfferingAndValidateOwner(resource.getOfferingId(), username);

        resourceRepository.deleteById(id);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取开课记录并校验当前用户是否为该记录的授课教师
     */
    private CourseOffering getOfferingAndValidateOwner(Long offeringId, String username) {
        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.UNAUTHORIZED, "用户不存在"));

        CourseOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "开课记录不存在"));

        if (!offering.getTeacherId().equals(teacher.getId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN, "无权操作该开课记录");
        }

        return offering;
    }

    private CourseResourceResponse toResponse(CourseResource resource) {
        CourseResourceResponse resp = new CourseResourceResponse();
        resp.setId(resource.getId());
        resp.setOfferingId(resource.getOfferingId());
        resp.setFileName(resource.getFileName());
        resp.setFilePath(resource.getFilePath());
        resp.setResourceType(resource.getResourceType());
        resp.setUploadTime(resource.getUploadTime() != null ? resource.getUploadTime().format(DTF) : null);
        return resp;
    }
}
