package com.food.common.convert;

import com.food.common.util.DesensitizeUtil;
import com.food.dto.UserAddDTO;
import com.food.entity.UserDO;
import com.food.vo.UserDetailVO;
import com.food.vo.UserListVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户对象转换器
 * <p>
 * 负责用户模块 DTO、DO、VO 之间的类型转换
 * 包含敏感字段脱敏处理
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public class UserConvertor {

    private UserConvertor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * UserAddDTO 转 UserDO
     * <p>
     * 密码使用 BCrypt 加密，设置默认状态为启用
     *
     * @param dto    用户添加请求
     * @param encoder 密码编码器
     * @return 用户DO
     */
    public static UserDO toDO(UserAddDTO dto, BCryptPasswordEncoder encoder) {
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender() != null ? dto.getGender() : 0);
        user.setStatus(1);
        user.setRemark(dto.getRemark());
        return user;
    }

    /**
     * UserDO 转 UserListVO（列表展示，敏感字段脱敏）
     *
     * @param user 用户DO
     * @return 用户列表VO
     */
    public static UserListVO toListVO(UserDO user) {
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(DesensitizeUtil.phone(user.getPhone()));
        vo.setEmail(DesensitizeUtil.email(user.getEmail()));
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    /**
     * UserDO 转 UserDetailVO（详情展示，敏感字段脱敏）
     *
     * @param user 用户DO
     * @return 用户详情VO
     */
    public static UserDetailVO toDetailVO(UserDO user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(DesensitizeUtil.phone(user.getPhone()));
        vo.setEmail(DesensitizeUtil.email(user.getEmail()));
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(DesensitizeUtil.ip(user.getLastLoginIp()));
        vo.setRemark(user.getRemark());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
