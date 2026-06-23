package com.certification.backend.controller.teacher;

import com.certification.backend.dto.request.CourseOfferingRequest;
import com.certification.backend.dto.request.PageQuery;
import com.certification.backend.dto.response.CourseOfferingResponse;
import com.certification.backend.dto.response.PageResult;
import com.certification.backend.dto.response.ResponseVO;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.CourseOfferingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 开课信息管理控制器（教师权限）
 */
@Tag(name = "04-开课信息管理", description = "教师端开课记录的增删改查")
@RestController
@RequestMapping("/teacher/offering")
@PreAuthorize("hasRole('TEACHER')")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public CourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @Operation(summary = "分页查询我的开课列表", description = "支持按课程关键词模糊搜索")
    @GetMapping("/list")
    public ResponseVO<PageResult<CourseOfferingResponse>> listOfferings(
            @RequestParam(required = false) String keyword,
            @Valid PageQuery pageQuery) {
        String username = getCurrentUsername();
        PageResult<CourseOfferingResponse> result = courseOfferingService.listOfferings(username, keyword, pageQuery);
        return ResponseVO.success(result);
    }

    @Operation(summary = "查询开课记录详情")
    @GetMapping("/detail/{id}")
    public ResponseVO<CourseOfferingResponse> detail(@PathVariable Long id) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseOfferingService.getOfferingDetail(id, username));
    }

    @Operation(summary = "新增开课记录")
    @PostMapping("/add")
    public ResponseVO<CourseOfferingResponse> add(@Valid @RequestBody CourseOfferingRequest request) {
        String username = getCurrentUsername();
        return ResponseVO.success(courseOfferingService.addOffering(request, username));
    }

    @Operation(summary = "编辑开课记录")
    @PutMapping("/update")
    public ResponseVO<CourseOfferingResponse> update(@Valid @RequestBody CourseOfferingRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "开课记录ID不能为空");
        }
        String username = getCurrentUsername();
        return ResponseVO.success(courseOfferingService.updateOffering(request, username));
    }

    @Operation(summary = "删除开课记录")
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        String username = getCurrentUsername();
        courseOfferingService.deleteOffering(id, username);
        return ResponseVO.success();
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
