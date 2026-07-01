import request from './request'

/**
 * 获取开课记录的学生名单
 * @param {number|string} offeringId
 * @returns {Promise}
 */
export function getStudentList(offeringId) {
    return request.get(`/teacher/student-course/list/${offeringId}`)
}

/**
 * 批量导入学生（手动输入学号）
 * @param {number|string} offeringId
 * @param {string[]} studentNos
 * @returns {Promise}
 */
export function importStudents(offeringId, studentNos) {
    return request.post(`/teacher/student-course/import/${offeringId}`, studentNos)
}

/**
 * 移除单个学生
 * @param {number|string} offeringId
 * @param {number|string} studentId
 * @returns {Promise}
 */
export function removeStudent(offeringId, studentId) {
    return request.delete(`/teacher/student-course/remove/${offeringId}/${studentId}`)
}