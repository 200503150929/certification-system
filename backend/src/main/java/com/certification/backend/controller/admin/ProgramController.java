package com.certification.backend.controller.admin;

import com.certification.backend.dto.request.*;
import com.certification.backend.dto.response.*;
import com.certification.backend.enums.ResultCodeEnum;
import com.certification.backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人才培养方案管理控制器（管理员权限）
 *
 * 涵盖：专业/培养方案、培养目标、毕业要求、指标点、培养目标-毕业要求支撑矩阵
 */
@Tag(name = "03-人才培养方案管理", description = "专业/培养方案、培养目标、毕业要求、指标点、支撑矩阵管理")
@RestController
@RequestMapping("/admin/program")
@PreAuthorize("hasRole('ADMIN')")
public class ProgramController {

    private final ProgramService programService;
    private final EducationalObjectiveService educationalObjectiveService;
    private final GraduationRequirementService graduationRequirementService;
    private final IndicatorPointService indicatorPointService;
    private final ObjectiveRequirementMatrixService objectiveRequirementMatrixService;

    public ProgramController(ProgramService programService,
                              EducationalObjectiveService educationalObjectiveService,
                              GraduationRequirementService graduationRequirementService,
                              IndicatorPointService indicatorPointService,
                              ObjectiveRequirementMatrixService objectiveRequirementMatrixService) {
        this.programService = programService;
        this.educationalObjectiveService = educationalObjectiveService;
        this.graduationRequirementService = graduationRequirementService;
        this.indicatorPointService = indicatorPointService;
        this.objectiveRequirementMatrixService = objectiveRequirementMatrixService;
    }

    // ==================== 专业/培养方案 ====================

    @Operation(summary = "分页查询专业列表", description = "支持按专业名称模糊搜索，按状态过滤")
    @GetMapping("/list")
    public ResponseVO<PageResult<ProgramResponse>> listPrograms(
            @RequestParam(required = false) String majorNameFuzzy,
            @RequestParam(required = false) String status,
            @Valid PageQuery pageQuery) {
        PageResult<ProgramResponse> result = programService.listPrograms(majorNameFuzzy, status, pageQuery);
        return ResponseVO.success(result);
    }

    @Operation(summary = "查询所有专业列表（不分页）", description = "用于下拉选择等场景")
    @GetMapping("/options")
    public ResponseVO<List<ProgramResponse>> listAllPrograms() {
        return ResponseVO.success(programService.listAllPrograms());
    }

    @Operation(summary = "查询专业详情")
    @GetMapping("/detail/{id}")
    public ResponseVO<ProgramDetailResponse> detail(@PathVariable Long id) {
        return ResponseVO.success(programService.getProgramDetail(id));
    }

    @Operation(summary = "查询专业基本信息")
    @GetMapping("/get/{id}")
    public ResponseVO<ProgramResponse> getById(@PathVariable Long id) {
        return ResponseVO.success(programService.getProgramById(id));
    }

    @Operation(summary = "新增专业")
    @PostMapping("/add")
    public ResponseVO<ProgramResponse> add(@Valid @RequestBody ProgramRequest request) {
        return ResponseVO.success(programService.addProgram(request));
    }

    @Operation(summary = "编辑专业")
    @PutMapping("/update")
    public ResponseVO<ProgramResponse> update(@Valid @RequestBody ProgramRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "专业ID不能为空");
        }
        return ResponseVO.success(programService.updateProgram(request));
    }

    @Operation(summary = "删除专业", description = "会级联删除培养目标、毕业要求、指标点等关联数据")
    @DeleteMapping("/delete/{id}")
    public ResponseVO<Void> delete(@PathVariable Long id) {
        programService.deleteProgram(id);
        return ResponseVO.success();
    }

    @Operation(summary = "发布专业", description = "将状态从 draft 改为 published")
    @PutMapping("/publish/{id}")
    public ResponseVO<Void> publish(@PathVariable Long id) {
        programService.publishProgram(id);
        return ResponseVO.success();
    }

    @Operation(summary = "取消发布", description = "将状态从 published 改为 draft")
    @PutMapping("/unpublish/{id}")
    public ResponseVO<Void> unpublish(@PathVariable Long id) {
        programService.unpublishProgram(id);
        return ResponseVO.success();
    }

    // ==================== 培养目标 ====================

    @Operation(summary = "查询培养目标列表", description = "根据培养方案ID查询")
    @GetMapping("/objectives/list/{programId}")
    public ResponseVO<List<EducationalObjectiveResponse>> listObjectives(@PathVariable Long programId) {
        return ResponseVO.success(educationalObjectiveService.listByProgramId(programId));
    }

    @Operation(summary = "查询培养目标详情")
    @GetMapping("/objectives/detail/{id}")
    public ResponseVO<EducationalObjectiveResponse> getObjective(@PathVariable Long id) {
        return ResponseVO.success(educationalObjectiveService.getById(id));
    }

    @Operation(summary = "新增培养目标")
    @PostMapping("/objectives/add")
    public ResponseVO<EducationalObjectiveResponse> addObjective(@Valid @RequestBody EducationalObjectiveRequest request) {
        return ResponseVO.success(educationalObjectiveService.add(request));
    }

    @Operation(summary = "编辑培养目标")
    @PutMapping("/objectives/update")
    public ResponseVO<EducationalObjectiveResponse> updateObjective(@Valid @RequestBody EducationalObjectiveRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "培养目标ID不能为空");
        }
        return ResponseVO.success(educationalObjectiveService.update(request));
    }

    @Operation(summary = "删除培养目标")
    @DeleteMapping("/objectives/delete/{id}")
    public ResponseVO<Void> deleteObjective(@PathVariable Long id) {
        educationalObjectiveService.delete(id);
        return ResponseVO.success();
    }

    // ==================== 毕业要求 ====================

    @Operation(summary = "查询毕业要求列表", description = "根据培养方案ID查询")
    @GetMapping("/requirements/list/{programId}")
    public ResponseVO<List<GraduationRequirementResponse>> listRequirements(@PathVariable Long programId) {
        return ResponseVO.success(graduationRequirementService.listByProgramId(programId));
    }

    @Operation(summary = "查询毕业要求详情列表（含指标点）", description = "根据培养方案ID查询，包含每个毕业要求下的指标点")
    @GetMapping("/requirements/detail-list/{programId}")
    public ResponseVO<List<GraduationRequirementDetailResponse>> listRequirementDetails(@PathVariable Long programId) {
        return ResponseVO.success(graduationRequirementService.listDetailByProgramId(programId));
    }

    @Operation(summary = "查询毕业要求详情")
    @GetMapping("/requirements/detail/{id}")
    public ResponseVO<GraduationRequirementResponse> getRequirement(@PathVariable Long id) {
        return ResponseVO.success(graduationRequirementService.getById(id));
    }

    @Operation(summary = "新增毕业要求")
    @PostMapping("/requirements/add")
    public ResponseVO<GraduationRequirementResponse> addRequirement(@Valid @RequestBody GraduationRequirementRequest request) {
        return ResponseVO.success(graduationRequirementService.add(request));
    }

    @Operation(summary = "编辑毕业要求")
    @PutMapping("/requirements/update")
    public ResponseVO<GraduationRequirementResponse> updateRequirement(@Valid @RequestBody GraduationRequirementRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "毕业要求ID不能为空");
        }
        return ResponseVO.success(graduationRequirementService.update(request));
    }

    @Operation(summary = "删除毕业要求", description = "会级联删除下属指标点")
    @DeleteMapping("/requirements/delete/{id}")
    public ResponseVO<Void> deleteRequirement(@PathVariable Long id) {
        graduationRequirementService.delete(id);
        return ResponseVO.success();
    }

    // ==================== 指标点 ====================

    @Operation(summary = "查询指标点列表", description = "根据毕业要求ID查询")
    @GetMapping("/indicators/list/{requirementId}")
    public ResponseVO<List<IndicatorPointResponse>> listIndicators(@PathVariable Long requirementId) {
        return ResponseVO.success(indicatorPointService.listByRequirementId(requirementId));
    }

    @Operation(summary = "查询指标点详情")
    @GetMapping("/indicators/detail/{id}")
    public ResponseVO<IndicatorPointResponse> getIndicator(@PathVariable Long id) {
        return ResponseVO.success(indicatorPointService.getById(id));
    }

    @Operation(summary = "新增指标点")
    @PostMapping("/indicators/add")
    public ResponseVO<IndicatorPointResponse> addIndicator(@Valid @RequestBody IndicatorPointRequest request) {
        return ResponseVO.success(indicatorPointService.add(request));
    }

    @Operation(summary = "编辑指标点")
    @PutMapping("/indicators/update")
    public ResponseVO<IndicatorPointResponse> updateIndicator(@Valid @RequestBody IndicatorPointRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "指标点ID不能为空");
        }
        return ResponseVO.success(indicatorPointService.update(request));
    }

    @Operation(summary = "删除指标点")
    @DeleteMapping("/indicators/delete/{id}")
    public ResponseVO<Void> deleteIndicator(@PathVariable Long id) {
        indicatorPointService.delete(id);
        return ResponseVO.success();
    }

    // ==================== 培养目标-毕业要求支撑矩阵 ====================

    @Operation(summary = "查询支撑矩阵列表", description = "根据培养目标ID查询毕业要求支撑关系")
    @GetMapping("/matrix/list/{objectiveId}")
    public ResponseVO<List<ObjectiveRequirementMatrixResponse>> listMatrix(@PathVariable Long objectiveId) {
        return ResponseVO.success(objectiveRequirementMatrixService.listByObjectiveId(objectiveId));
    }

    @Operation(summary = "批量保存支撑矩阵", description = "全量替换某培养目标的毕业要求支撑关系（先删后插）")
    @PostMapping("/matrix/batch-save")
    public ResponseVO<List<ObjectiveRequirementMatrixResponse>> batchSaveMatrix(
            @Valid @RequestBody ObjectiveRequirementMatrixBatchRequest request) {
        return ResponseVO.success(objectiveRequirementMatrixService.batchSave(request));
    }

    @Operation(summary = "新增支撑关系")
    @PostMapping("/matrix/add")
    public ResponseVO<ObjectiveRequirementMatrixResponse> addMatrix(
            @Valid @RequestBody ObjectiveRequirementMatrixRequest request) {
        return ResponseVO.success(objectiveRequirementMatrixService.add(request));
    }

    @Operation(summary = "编辑支撑关系")
    @PutMapping("/matrix/update")
    public ResponseVO<ObjectiveRequirementMatrixResponse> updateMatrix(
            @Valid @RequestBody ObjectiveRequirementMatrixRequest request) {
        if (request.getId() == null) {
            return ResponseVO.error(ResultCodeEnum.BAD_REQUEST.getCode(), "支撑关系ID不能为空");
        }
        return ResponseVO.success(objectiveRequirementMatrixService.update(request));
    }

    @Operation(summary = "删除支撑关系")
    @DeleteMapping("/matrix/delete/{id}")
    public ResponseVO<Void> deleteMatrix(@PathVariable Long id) {
        objectiveRequirementMatrixService.delete(id);
        return ResponseVO.success();
    }
}