package com.certification.backend.service;

import com.certification.backend.dto.request.CourseResourceRequest;
import com.certification.backend.dto.response.CourseResourceResponse;
import com.certification.backend.entity.CourseResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程资源管理业务接口
 */
public interface CourseResourceService {

    /**
     * 新增课程资源（手动填写资源信息，不含文件上传）
     * 仅授课教师可操作
     */
    CourseResourceResponse add(CourseResourceRequest request, String username);

    /**
     * 上传课程资源文件
     * 仅授课教师可操作
     *
     * @param offeringId   开课记录 ID
     * @param file         上传的文件（仅允许 PDF/PPT/PPTX/DOC/DOCX/XLS/XLSX，最大 50MB）
     * @param resourceType 资源类型标签（如：课件、教案、参考资料）
     * @param username     当前教师用户名
     * @return 上传后的资源响应
     */
    CourseResourceResponse upload(Long offeringId, MultipartFile file, String resourceType, String username);

    /**
     * 根据 ID 获取资源实体（带访问权限校验）
     * 仅限选修该课程的学生或授课教师可访问
     *
     * @param id       资源 ID
     * @param username 当前用户名
     * @return 课程资源实体
     */
    CourseResource getByIdWithAccessCheck(Long id, String username);

    /**
     * 根据 ID 获取资源实体（无权限校验，仅供内部使用）
     *
     * @param id 资源 ID
     * @return 课程资源实体
     */
    CourseResource getById(Long id);

    /**
     * 查询指定开课记录下的所有资源
     * 仅限选修该课程的学生或授课教师可查看
     */
    List<CourseResourceResponse> listByOfferingId(Long offeringId, String username);

    /**
     * 删除指定资源（同时删除磁盘文件）
     * 仅授课教师可操作
     */
    void delete(Long id, String username);
}