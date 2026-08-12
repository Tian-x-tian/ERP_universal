package com.erp.system.support;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;

/**
 * MDM 乐观锁辅助工具。
 */
public final class MdmOptimisticLockSupport {

    private MdmOptimisticLockSupport() {
    }

    /**
     * 校验请求版本号与数据库当前版本号是否一致。
     *
     * @param requestVersion 请求版本号
     * @param currentVersion 当前版本号
     * @param label          业务名称
     * @return 通过校验的版本号
     */
    public static Integer requireVersion(Integer requestVersion, Integer currentVersion, String label) {
        if (requestVersion == null) {
            throw new ServiceException(label + "版本号不能为空，请刷新后重试", (int) ResultCode.VALIDATE_FAILED.getCode());
        }
        if (currentVersion == null || !requestVersion.equals(currentVersion)) {
            throw new ServiceException(label + "数据已被其他人更新，请刷新后重试", (int) ResultCode.CONFLICT.getCode());
        }
        return requestVersion;
    }

    /**
     * 统一处理版本更新失败的场景。
     *
     * @param updated 是否更新成功
     * @param label   业务名称
     */
    public static void ensureUpdated(boolean updated, String label) {
        if (!updated) {
            throw new ServiceException(label + "数据已被其他人更新，请刷新后重试", (int) ResultCode.CONFLICT.getCode());
        }
    }
}
