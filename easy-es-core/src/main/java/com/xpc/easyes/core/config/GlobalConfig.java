package com.xpc.easyes.core.config;

import com.xpc.easyes.core.enums.ProcessIndexStrategyEnum;
import com.xpc.easyes.core.enums.FieldStrategy;
import com.xpc.easyes.core.enums.IdType;
import lombok.Data;

/**
 * 全局配置
 * <p>
 * Copyright © 2021 xpc1024 All Rights Reserved
 **/
@Data
public class GlobalConfig {
    /**
     * 是否开启本框架执行的DSL语句控制台打印 默认开启,生产环境建议关闭
     */
    private boolean printDsl = true;
    /**
     * 自动托管索引模式 默认为平滑迁移
     */
    private Integer processIndexMode = ProcessIndexStrategyEnum.SMOOTHLY.getStrategyType();
    /**
     * 数据库配置
     */
    private DbConfig dbConfig;

    @Data
    public static class DbConfig {
        /**
         * 主键类型（默认 AUTO）
         */
        private IdType idType = IdType.AUTO;
        /**
         * 索引前缀
         */
        private String tablePrefix;
        /**
         * 是否开启下划线转驼峰 默认不开启,与MP相反
         */
        private boolean mapUnderscoreToCamelCase = false;
        /**
         * 字段验证策略 (默认 NOT NULL)
         */
        private FieldStrategy fieldStrategy = FieldStrategy.NOT_NULL;
        /**
         * 是否开启自动托管索引 默认开启
         */
        private boolean openAutoProcessIndex = true;
        /**
         * 是否分布式环境
         */
        private boolean isDistributed = false;
    }
}
