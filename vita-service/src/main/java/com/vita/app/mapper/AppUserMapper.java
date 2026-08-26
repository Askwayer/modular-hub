package com.vita.app.mapper;

import com.vita.app.entity.AppUser;
import com.vita.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.app.mapper
 * @Author znk
 * @CreateTime 2026-08-26 16:58:43
 * @Description: App用户表 Mapper 接口
 * @version 1.0
 */
@Mapper
public interface AppUserMapper extends BaseMapperX<AppUser> {

}
