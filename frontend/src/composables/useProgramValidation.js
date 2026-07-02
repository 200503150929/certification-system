import request from '@/api/request'

export function useProgramValidation() {

    /**
     * 校验培养方案数据完整性
     * @param {string|number} programId - 专业ID
     * @returns {Promise<{ errors: Array, warnings: Array }>}
     */
    const validateProgramData = async (programId) => {
        const errors = []

        // ============ 1. 检查培养目标 ============
        try {
            const goalsRes = await request.get(`/admin/program/objectives/list/${programId}`)
            const goals = goalsRes.data || []

            if (goals.length === 0) {
                errors.push({
                    field: 'goals',
                    message: '请至少配置一个培养目标',
                    tab: 'goals'
                })
            }
        } catch (e) {
            errors.push({
                field: 'goals',
                message: '获取培养目标数据失败，请稍后重试',
                tab: 'goals'
            })
        }

        // ============ 2. 检查毕业要求及指标点 ============
        let requirements = []
        let allIndicators = []

        try {
            const reqRes = await request.get(`/admin/program/requirements/list/${programId}`)
            requirements = reqRes.data || []

            if (requirements.length === 0) {
                errors.push({
                    field: 'requirements',
                    message: '请至少配置一条毕业要求',
                    tab: 'requirements'
                })
            } else {
                // 检查每个毕业要求是否有指标点
                let hasIndicatorError = false
                for (const req of requirements) {
                    try {
                        const indRes = await request.get(`/admin/program/indicators/list/${req.id}`)
                        const indicators = indRes.data || []
                        allIndicators.push(...indicators.map(ind => ({ ...ind, requirementId: req.id })))

                        if (indicators.length === 0 && !hasIndicatorError) {
                            errors.push({
                                field: 'indicators',
                                message: `毕业要求 "${req.code}" 缺少指标点配置`,
                                tab: 'requirements'
                            })
                            hasIndicatorError = true
                        }
                    } catch (e) {
                        errors.push({
                            field: 'indicators',
                            message: `获取毕业要求 "${req.code}" 的指标点失败`,
                            tab: 'requirements'
                        })
                    }
                }
            }
        } catch (e) {
            errors.push({
                field: 'requirements',
                message: '获取毕业要求数据失败，请稍后重试',
                tab: 'requirements'
            })
        }

        // ============ 3. 检查课程体系 ============
        let courses = []
        try {
            const courseRes = await request.get('/admin/course/list', {
                params: {
                    programId: programId,
                    pageNum: 1,
                    pageSize: 999
                }
            })
            courses = courseRes.data?.list || []

            if (courses.length === 0) {
                errors.push({
                    field: 'courses',
                    message: '请至少添加一门课程',
                    tab: 'courses'
                })
            }
        } catch (e) {
            errors.push({
                field: 'courses',
                message: '获取课程数据失败，请稍后重试',
                tab: 'courses'
            })
        }

        // ============ 4. 检查目标-要求矩阵 ============
        try {
            const goalsRes = await request.get(`/admin/program/objectives/list/${programId}`)
            const goals = goalsRes.data || []

            if (goals.length > 0 && requirements.length > 0) {
                let hasMatrixError = false

                for (const goal of goals) {
                    try {
                        const matrixRes = await request.get(`/admin/program/matrix/list/${goal.id}`)
                        const items = matrixRes.data || []

                        // 检查该目标是否至少支撑了一条毕业要求
                        const hasValidSupport = items.some(item =>
                            ['H', 'M', 'L'].includes(item.supportLevel)
                        )

                        if (!hasValidSupport && !hasMatrixError) {
                            const shortDesc = goal.description?.length > 15
                                ? goal.description.substring(0, 15) + '...'
                                : goal.description
                            errors.push({
                                field: 'objectiveMatrix',
                                message: `培养目标 "${shortDesc}" 未配置任何支撑关系`,
                                tab: 'objectiveMatrix'
                            })
                            hasMatrixError = true
                        }
                    } catch (e) {
                        errors.push({
                            field: 'objectiveMatrix',
                            message: '获取目标-要求矩阵数据失败，请稍后重试',
                            tab: 'objectiveMatrix'
                        })
                    }
                }
            }
        } catch (e) {
            // 如果获取目标失败，前面已经报错了，这里忽略
        }

        // ============ 5. 检查课程-要求矩阵 ============
        try {
            if (courses.length > 0 && allIndicators.length > 0) {
                let hasMatrixError = false

                for (const course of courses) {
                    try {
                        const detailRes = await request.get(`/admin/course/detail/${course.id}`)
                        const matrixItems = detailRes.data?.matrixItems || []

                        // 检查该课程是否至少支撑了一个指标点
                        const hasValidSupport = matrixItems.some(item =>
                            ['H', 'M', 'L'].includes(item.supportLevel)
                        )

                        if (!hasValidSupport && !hasMatrixError) {
                            errors.push({
                                field: 'courseMatrix',
                                message: `课程 "${course.name}" 未配置任何指标点支撑关系`,
                                tab: 'courseMatrix'
                            })
                            hasMatrixError = true
                        }
                    } catch (e) {
                        errors.push({
                            field: 'courseMatrix',
                            message: `获取课程 "${course.name}" 的支撑矩阵失败`,
                            tab: 'courseMatrix'
                        })
                    }
                }
            } else if (courses.length > 0 && allIndicators.length === 0) {
                // 如果有课程但没有指标点，前面已经报错了
                // 这里不再重复添加
            }
        } catch (e) {
            // 忽略
        }

        return { errors }
    }

    /**
     * 获取校验错误的 Tab 映射
     */
    const getErrorTabMap = (errors) => {
        const tabMap = {}
        for (const error of errors) {
            if (error.tab && !tabMap[error.tab]) {
                tabMap[error.tab] = true
            }
        }
        return Object.keys(tabMap)
    }

    return {
        validateProgramData,
        getErrorTabMap
    }
}