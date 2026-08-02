package com.erp.business.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.business.saas.domain.SaasStorageObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SaasStorageObjectMapper extends BaseMapper<SaasStorageObject> {
    @Select("SELECT * FROM biz_saas_storage_object WHERE tenant_id = #{tenantId} AND object_key = #{objectKey}")
    SaasStorageObject find(@Param("tenantId") String tenantId, @Param("objectKey") String objectKey);

    @Update("UPDATE biz_saas_storage_object SET status = 'ACTIVE', last_error = NULL, update_time = NOW(3) "
            + "WHERE tenant_id = #{tenantId} AND object_key = #{objectKey} AND status = 'UPLOADING'")
    int markActive(@Param("tenantId") String tenantId, @Param("objectKey") String objectKey);

    @Update("UPDATE biz_saas_storage_object SET status = #{status}, last_error = #{lastError}, "
            + "update_time = NOW(3) WHERE tenant_id = #{tenantId} AND object_key = #{objectKey} "
            + "AND status <> 'DELETED'")
    int markTerminal(@Param("tenantId") String tenantId, @Param("objectKey") String objectKey,
            @Param("status") String status, @Param("lastError") String lastError);
}
