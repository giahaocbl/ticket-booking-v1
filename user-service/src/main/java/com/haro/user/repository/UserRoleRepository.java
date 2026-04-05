package com.haro.user.repository;

import com.haro.user.entity.User;
import com.haro.user.entity.UserRole;
import com.haro.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUser(User user);
}
