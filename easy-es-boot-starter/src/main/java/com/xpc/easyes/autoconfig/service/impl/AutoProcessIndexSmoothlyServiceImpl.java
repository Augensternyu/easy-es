package com.xpc.easyes.autoconfig.service.impl;

import com.xpc.easyes.autoconfig.service.AutoProcessIndexService;
import com.xpc.easyes.core.common.EntityInfo;
import com.xpc.easyes.core.enums.ProcessIndexStrategyEnum;
import com.xpc.easyes.core.params.CreateIndexParam;
import com.xpc.easyes.core.params.EsIndexInfo;
import com.xpc.easyes.core.toolkit.EntityInfoHelper;
import com.xpc.easyes.core.toolkit.IndexUtils;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import static com.xpc.easyes.core.constants.BaseEsConstants.S1_SUFFIX;
import static com.xpc.easyes.core.constants.BaseEsConstants.SO_SUFFIX;

/**
 * 自动平滑托管索引实现类,本框架默认模式,过程零停机,数据会自动转移至新索引
 * <p>
 * Copyright © 2022 xpc1024 All Rights Reserved
 **/
@Service
@ConditionalOnClass(RestHighLevelClient.class)
@ConditionalOnProperty(prefix = "easy-es", name = {"enable"}, havingValue = "true", matchIfMissing = true)
public class AutoProcessIndexSmoothlyServiceImpl implements AutoProcessIndexService {
    @Override
    public Integer getStrategyType() {
        return ProcessIndexStrategyEnum.SMOOTHLY.getStrategyType();
    }

    @Override
    public void processIndexAsync(Class<?> entityClass, RestHighLevelClient client) {
        IndexUtils.supplyAsync(this::process, entityClass, client);
    }


    private synchronized boolean process(Class<?> entityClass, RestHighLevelClient client) {
        EntityInfo entityInfo = EntityInfoHelper.getEntityInfo(entityClass);

        // 索引是否已存在
        boolean existsIndex = IndexUtils.existsIndexWithRetryAndSetActiveIndex(entityInfo, client);
        if (existsIndex) {
            // 更新
            return doUpdateIndex(entityInfo, client);
        } else {
            // 新建
            return doCreateIndex(entityInfo, client);
        }
    }


    private boolean doUpdateIndex(EntityInfo entityInfo, RestHighLevelClient client) {
        // 获取索引信息
        EsIndexInfo esIndexInfo = IndexUtils.getIndex(client, entityInfo.getIndexName());

        // 是否存在默认别名,若无则给添加
        if (!esIndexInfo.getHasDefaultAlias()) {
            IndexUtils.addDefaultAlias(client, entityInfo.getIndexName());
        }

        // 索引是否有变化 若有则创建新索引并无感迁移, 若无则直接返回托管成功
        boolean isIndexNeedChange = IndexUtils.isIndexNeedChange(esIndexInfo, entityInfo);
        if (!isIndexNeedChange) {
            return Boolean.TRUE;
        }

        // 创建新索引
        String releaseIndexName = generateReleaseIndexName(entityInfo.getIndexName());
        entityInfo.setReleaseIndexName(releaseIndexName);
        boolean isCreateIndexSuccess = doCreateIndex(entityInfo, client);
        if (!isCreateIndexSuccess) {
            return Boolean.FALSE;
        }

        //  迁移数据至新创建的索引
        boolean isDataMigrationSuccess = doDataMigration(entityInfo.getIndexName(), releaseIndexName, client);
        if (!isDataMigrationSuccess) {
            return Boolean.FALSE;
        }

        // 原子操作 切换别名:将默认别名关联至新索引,并将旧索引的默认别名移除
        boolean isChangeAliasSuccess = IndexUtils.changeAliasAtomic(client, entityInfo.getIndexName(), releaseIndexName);
        if (!isChangeAliasSuccess) {
            return Boolean.FALSE;
        }

        // 删除旧索引
        boolean isDeletedIndexSuccess = IndexUtils.deleteIndex(client, entityInfo.getIndexName());
        if (!isDeletedIndexSuccess) {
            return Boolean.FALSE;
        }

        // 用最新索引覆盖缓存中的老索引
        entityInfo.setIndexName(releaseIndexName);

        // done.
        return Boolean.TRUE;
    }

    private String generateReleaseIndexName(String oldIndexName) {
        if (oldIndexName.endsWith(SO_SUFFIX)) {
            return oldIndexName.split(SO_SUFFIX)[0] + S1_SUFFIX;
        } else if (oldIndexName.endsWith(S1_SUFFIX)) {
            return oldIndexName.split(S1_SUFFIX)[0] + SO_SUFFIX;
        } else {
            return oldIndexName + SO_SUFFIX;
        }
    }

    private boolean doDataMigration(String oldIndexName, String releaseIndexName, RestHighLevelClient client) {
        return IndexUtils.reindex(client, oldIndexName, releaseIndexName);
    }


    private boolean doCreateIndex(EntityInfo entityInfo, RestHighLevelClient client) {
        // 初始化创建索引参数
        CreateIndexParam createIndexParam = IndexUtils.getCreateIndexParam(entityInfo);

        // 执行创建
        return IndexUtils.createIndex(client, createIndexParam);
    }

}
