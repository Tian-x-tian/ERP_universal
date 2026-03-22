package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysUserPost;
import com.erp.workflow.mapper.SysUserPostMapper;
import com.erp.workflow.service.ISysUserPostService;
import org.springframework.stereotype.Service;

/**
 * 工作流内部用户岗位关联服务实现。
 */
@Service
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements ISysUserPostService {
}
