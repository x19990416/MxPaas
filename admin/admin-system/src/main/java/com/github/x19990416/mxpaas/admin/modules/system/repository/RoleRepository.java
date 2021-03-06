/*
 *  Copyright (c) 2020-2021 Guo Limin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.x19990416.mxpaas.admin.modules.system.repository;

import com.github.x19990416.mxpaas.admin.modules.system.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
  public Role findByName(String name);

  void deleteAllByIdIn(Set<Long> ids);

  @Query(
      value =
          "SELECT r.* FROM sys_role r, sys_users_roles u WHERE "
              + "r.role_id = u.role_id AND u.user_id = ?1",
      nativeQuery = true)
  Set<Role> findByUserId(Long id);

  @Modifying
  @Query(value = "delete from sys_roles_menus where menu_id = ?1", nativeQuery = true)
  void untiedMenu(Long id);

  @Query(
      value =
          "SELECT r.* FROM sys_role r, sys_roles_menus m WHERE "
              + "r.role_id = m.role_id AND m.menu_id in ?1",
      nativeQuery = true)
  List<Role> findInMenuId(List<Long> menuIds);

  @Query(value = "select count(1) from sys_role r, sys_roles_depts d where " +
          "r.role_id = d.role_id and d.dept_id in ?1",nativeQuery = true)
  int countByDepts(Set<Long> deptIds);
}
