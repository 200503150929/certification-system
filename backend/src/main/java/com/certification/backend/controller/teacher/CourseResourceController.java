package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.CourseResourceRequest;
import com.certification.backend.dto.response.CourseResourceResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.entity.CourseResource;
import com.certification.backend.service.CourseResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 课程资源管理控制器（教师权限）
 */
@Tag(name = "07-课程资源管理", description = "教师端课程资源的增删查")
@RestController
@RequestMapping("/teacher/resources")
@PreAuthorize("hasRole('TEACHER')")
public class CourseResourceController {

    private final CourseResourceService courseResourceService;

    public CourseResourceController(CourseResourceService courseResourceService) {
        this.courseResourceService = courseResourceService;
    }

    @Operation(summary = "新增课程资源（手动填写资源信息）")
    @PostMapping
    public ResponseVO<CourseResourceResponse> add(@Valid @RequestBody CourseResourceRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseResourceService.add(request, username));
    }

    @Operation(summary = "上传课程资源文件",
            description = "上传文件至服务器，仅允许 PDF/PPT/PPTX/DOC/DOCX/XLS/XLSX，最大 50MB")
    @PostMapping("/upload/{offeringId}")
    public ResponseVO<CourseResourceResponse> upload(
            @PathVariable Long offeringId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resourceType", required = false) String resourceType) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseResourceService.upload(offeringId, file, resourceType, username));
    }

    @Operation(summary = "下载课程资源文件",
            description = "根据资源 ID 下载对应的文件，任何已登录用户均可访问")
    @GetMapping("/download/{id}")
    @PreAuthorize("isAuthenticated()")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        CourseResource resource = courseResourceService.getById(id);
        Path filePath = Paths.get("uploads/course", resource.getFilePath());

        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // 设置响应头
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        response.setContentLengthLong(Files.size(filePath));

        String encodedFileName = URLEncoder.encode(resource.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        // 流式写出文件
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
            out.flush();
        }
    }

    @Operation(summary = "查询某门开课的所有资源")
    @GetMapping("/offering/{offeringId}")
    public ResponseVO<List<CourseResourceResponse>> listByOffering(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseResourceService.listByOfferingId(offeringId, username));
    }

    @Operation(summary = "删除指定的课程资源")
    @DeleteMapping("/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        String username = getCurrentUsername();
        courseResourceService.delete(id, username);
        return ResponseVO.success();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
