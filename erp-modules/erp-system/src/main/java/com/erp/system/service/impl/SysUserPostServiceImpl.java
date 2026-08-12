package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysUserPost;
import com.erp.system.mapper.SysUserPostMapper;
import com.erp.system.service.ISysUserPostService;
import org.springframework.stereotype.Service;

/**
 * 用户岗位关联服务实现
 */
@Service
public class SysUserPostServiceImpl extends ServiceImpl<SysUserPostMapper, SysUserPost> implements ISysUserPostService {
}
