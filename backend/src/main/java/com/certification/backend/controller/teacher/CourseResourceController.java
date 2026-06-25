package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.CourseResourceRequest;
import com.certification.backend.dto.response.CourseResourceResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.CourseResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "新增课程资源")
    @PostMapping
    public ResponseVO<CourseResourceResponse> add(@Valid @RequestBody CourseResourceRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseResourceService.add(request, username));
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
