package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysPost;
import com.erp.system.mapper.SysPostMapper;
import com.erp.system.service.ISysPostService;
import org.springframework.stereotype.Service;

/**
 * 岗位服务实现
 */
@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {
}
