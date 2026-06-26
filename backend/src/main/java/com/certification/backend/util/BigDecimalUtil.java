package com.certification.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * BigDecimal 精确计算工具类
 * 所有计算结果保留4位小数，使用 HALF_UP 舍入模式
 */
public class BigDecimalUtil {

    /** 小数位数 */
    private static final int SCALE = 4;

    /** 舍入模式 */
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** 满分100 */
    public static final BigDecimal FULL_SCORE = new BigDecimal("100");

    private BigDecimalUtil() {
    }

    /**
     * 保留4位小数
     */
    public static BigDecimal format(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(SCALE, ROUNDING);
    }

    /**
     * 除法运算（保留4位小数）
     */
    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, SCALE, ROUNDING);
    }

    /**
     * 乘法运算（保留4位小数）
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO;
        }
        return a.multiply(b).setScale(SCALE, ROUNDING);
    }

    /**
     * 求和（保留4位小数）
     */
    public static BigDecimal sum(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total.setScale(SCALE, ROUNDING);
    }

    /**
     * 求平均值（保留4位小数）
     */
    public static BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = sum(values);
        return divide(total, new BigDecimal(values.size()));
    }

    /**
     * 将分数转换为达成度（0~1之间的值，保留4位小数）
     * 达成度 = 分数 / 100
     */
    public static BigDecimal scoreToAchievement(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        return divide(score, FULL_SCORE);
    }
}
