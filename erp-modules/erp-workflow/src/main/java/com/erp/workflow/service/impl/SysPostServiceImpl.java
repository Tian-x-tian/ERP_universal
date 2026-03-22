package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysPost;
import com.erp.workflow.mapper.SysPostMapper;
import com.erp.workflow.service.ISysPostService;
import org.springframework.stereotype.Service;

/**
 * 工作流内部岗位只读服务实现。
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {
}
