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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 课程资源管理业务实现
 */
@Service
@Transactional
public class CourseResourceServiceImpl implements CourseResourceService {

    private static final Logger log = LoggerFactory.getLogger(CourseResourceServiceImpl.class);

    private final CourseResourceRepository resourceRepository;
    private final CourseOfferingRepository offeringRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 上传文件存储根目录（相对于项目运行目录） */
    private static final String UPLOAD_BASE_DIR = "uploads/course";

    /** 允许上传的文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "ppt", "pptx", "doc", "docx", "xls", "xlsx"
    );

    /** 最大文件大小：50MB */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

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
    public CourseResourceResponse upload(Long offeringId, MultipartFile file, String resourceType, String username) {
        // 校验开课记录权限
        getOfferingAndValidateOwner(offeringId, username);

        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "上传文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "文件大小不能超过 50MB");
        }

        // 获取并校验文件扩展名
        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST,
                    "不支持的文件类型，仅允许：PDF、PPT、PPTX、DOC、DOCX、XLS、XLSX");
        }

        // 生成唯一文件名，防止冲突
        String uniqueFileName = UUID.randomUUID().toString().replace("-", "") + "." + extension.toLowerCase();
        String subDir = String.valueOf(offeringId);
        Path targetDir = Paths.get(UPLOAD_BASE_DIR, subDir);

        try {
            // 确保目录存在
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件上传成功：{}", targetFile);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResultCodeEnum.BAD_REQUEST.getCode(), "文件保存失败：" + e.getMessage());
        }

        // 构造相对访问路径（用于拼接 URL）
        String relativePath = subDir + "/" + uniqueFileName;

        CourseResource resource = new CourseResource();
        resource.setOfferingId(offeringId);
        resource.setFileName(originalFileName != null ? originalFileName : uniqueFileName);
        resource.setFilePath(relativePath);
        resource.setResourceType(resourceType);

        CourseResource saved = resourceRepository.save(resource);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResource getById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程资源不存在"));
    }

    @Override
    @Transactional
    public void delete(Long id, String username) {
        CourseResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCodeEnum.NOT_FOUND, "课程资源不存在"));

        // 校验该资源所属的开课记录是否属于当前教师
        getOfferingAndValidateOwner(resource.getOfferingId(), username);

        // 尝试删除磁盘上的物理文件
        try {
            Path filePath = Paths.get(UPLOAD_BASE_DIR, resource.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("已删除磁盘文件：{}", filePath);
            }
        } catch (IOException e) {
            log.warn("删除磁盘文件失败（继续删除数据库记录）：{}", e.getMessage());
        }

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

    /**
     * 从文件名中提取扩展名（不含点）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
