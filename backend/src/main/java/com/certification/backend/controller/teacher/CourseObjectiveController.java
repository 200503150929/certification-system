package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.CourseObjectiveRequest;
import com.certification.backend.dto.response.CourseObjectiveResponse;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.service.CourseObjectiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程目标管理控制器（教师权限）
 */
@Tag(name = "05-课程目标管理", description = "教师端课程目标的增删改查")
@RestController
@PreAuthorize("hasRole('TEACHER')")
public class CourseObjectiveController {

    private final CourseObjectiveService courseObjectiveService;

    public CourseObjectiveController(CourseObjectiveService courseObjectiveService) {
        this.courseObjectiveService = courseObjectiveService;
    }

    @Operation(summary = "查询开课记录下的所有课程目标")
    @GetMapping("/teacher/offering/{offeringId}/objectives")
    public ResponseVO<List<CourseObjectiveResponse>> list(@PathVariable Long offeringId) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseObjectiveService.listByOfferingId(offeringId, username));
    }

    @Operation(summary = "新增课程目标")
    @PostMapping("/teacher/offering/{offeringId}/objectives")
    public ResponseVO<CourseObjectiveResponse> add(@PathVariable Long offeringId,
                                                   @Valid @RequestBody CourseObjectiveRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseObjectiveService.add(offeringId, request, username));
    }

    @Operation(summary = "编辑课程目标")
    @PutMapping("/teacher/objectives/{id}")
    public ResponseVO<CourseObjectiveResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody CourseObjectiveRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseObjectiveService.update(id, request, username));
    }

    @Operation(summary = "删除课程目标")
    @DeleteMapping("/teacher/objectives/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        String username = getCurrentUsername();
        courseObjectiveService.delete(id, username);
        return ResponseVO.success();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
