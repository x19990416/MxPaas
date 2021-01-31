/** create by Guo Limin on 2021/1/30. */
package com.github.x19990416.mxpaas.admin.modules.system.service.impl;

import com.github.x19990416.mxpaas.admin.common.exception.BadRequestException;
import com.github.x19990416.mxpaas.admin.common.exception.EntityExistException;
import com.github.x19990416.mxpaas.admin.common.utils.*;
import com.github.x19990416.mxpaas.admin.modules.system.domain.Menu;
import com.github.x19990416.mxpaas.admin.modules.system.domain.Role;
import com.github.x19990416.mxpaas.admin.modules.system.domain.User;
import com.github.x19990416.mxpaas.admin.modules.system.domain.vo.MenuMetaVo;
import com.github.x19990416.mxpaas.admin.modules.system.domain.vo.MenuVo;
import com.github.x19990416.mxpaas.admin.modules.system.repository.MenuRepository;
import com.github.x19990416.mxpaas.admin.modules.system.repository.UserRepository;
import com.github.x19990416.mxpaas.admin.modules.system.service.MenuService;
import com.github.x19990416.mxpaas.admin.modules.system.service.RoleService;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.MenuDto;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.MenuMapper;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.MenuQueryCriteria;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.RoleSmallDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "menu")
public class MenuServiceImpl implements MenuService {

  private final MenuRepository menuRepository;
  private final UserRepository userRepository;
  private final MenuMapper menuMapper;
  private final RoleService roleService;
  private final RedisUtil redisUtils;

  @Override
  public List<MenuDto> queryAll(MenuQueryCriteria criteria, Boolean isQuery) throws Exception {
    Sort sort = Sort.by(Sort.Direction.ASC, "menuSort");
    if (isQuery) {
      criteria.setPidIsNull(true);
      List<Field> fields = QueryHelper.getAllFields(criteria.getClass(), new ArrayList<>());
      for (Field field : fields) {
        // 设置对象的访问权限，保证对private的属性的访问
        field.setAccessible(true);
        Object val = field.get(criteria);
        if ("pidIsNull".equals(field.getName())) {
          continue;
        }
        if (null == val) {
          criteria.setPidIsNull(null);
          break;
        }
      }
    }
    return menuMapper.toDto(
        menuRepository.findAll(
            (root, criteriaQuery, criteriaBuilder) ->
                QueryHelper.getPredicate(root, criteria, criteriaBuilder),
            sort));
  }

  @Override
  @Cacheable(key = "'id:' + #p0")
  public MenuDto findById(long id) {
    Menu menu = menuRepository.findById(id).orElseGet(Menu::new);
    ValidationUtil.isNull(menu.getId(), "Menu", "id", id);
    return menuMapper.toDto(menu);
  }

  /**
   * 用户角色改变时需清理缓存
   *
   * @param currentUserId /
   * @return /
   */
  @Override
  @Cacheable(key = "'user:' + #p0")
  public List<MenuDto> findByUser(Long currentUserId) {
    List<RoleSmallDto> roles = roleService.findByUsersId(currentUserId);
    Set<Long> roleIds = roles.stream().map(RoleSmallDto::getId).collect(Collectors.toSet());
    LinkedHashSet<Menu> menus = menuRepository.findByRoleIdsAndTypeNot(roleIds, 2);
    return menus.stream().map(menuMapper::toDto).collect(Collectors.toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void create(Menu resources) {
    if (menuRepository.findByTitle(resources.getTitle()) != null) {
      throw new EntityExistException(Menu.class, "title", resources.getTitle());
    }
    if (Strings.isNotBlank(resources.getComponentName())) {
      if (menuRepository.findByComponentName(resources.getComponentName()) != null) {
        throw new EntityExistException(Menu.class, "componentName", resources.getComponentName());
      }
    }
    if (resources.getPid().equals(0L)) {
      resources.setPid(null);
    }
    if (resources.getIFrame()) {
      String http = "http://", https = "https://";
      if (!(resources.getPath().toLowerCase().startsWith(http)
          || resources.getPath().toLowerCase().startsWith(https))) {
        throw new BadRequestException("外链必须以http://或者https://开头");
      }
    }
    menuRepository.save(resources);
    // 计算子节点数目
    resources.setSubCount(0);
    // 更新父节点菜单数目
    updateSubCnt(resources.getPid());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void update(Menu resources) {
    if (resources.getId().equals(resources.getPid())) {
      throw new BadRequestException("上级不能为自己");
    }
    Menu menu = menuRepository.findById(resources.getId()).orElseGet(Menu::new);
    ValidationUtil.isNull(menu.getId(), "Permission", "id", resources.getId());

    if (resources.getIFrame()) {
      String http = "http://", https = "https://";
      if (!(resources.getPath().toLowerCase().startsWith(http)
          || resources.getPath().toLowerCase().startsWith(https))) {
        throw new BadRequestException("外链必须以http://或者https://开头");
      }
    }
    Menu menu1 = menuRepository.findByTitle(resources.getTitle());

    if (menu1 != null && !menu1.getId().equals(menu.getId())) {
      throw new EntityExistException(Menu.class, "title", resources.getTitle());
    }

    if (resources.getPid().equals(0L)) {
      resources.setPid(null);
    }

    // 记录的父节点ID
    Long oldPid = menu.getPid();
    Long newPid = resources.getPid();

    if (Strings.isNotBlank(resources.getComponentName())) {
      menu1 = menuRepository.findByComponentName(resources.getComponentName());
      if (menu1 != null && !menu1.getId().equals(menu.getId())) {
        throw new EntityExistException(Menu.class, "componentName", resources.getComponentName());
      }
    }
    menu.setTitle(resources.getTitle());
    menu.setComponent(resources.getComponent());
    menu.setPath(resources.getPath());
    menu.setIcon(resources.getIcon());
    menu.setIFrame(resources.getIFrame());
    menu.setPid(resources.getPid());
    menu.setMenuSort(resources.getMenuSort());
    menu.setCache(resources.getCache());
    menu.setHidden(resources.getHidden());
    menu.setComponentName(resources.getComponentName());
    menu.setPermission(resources.getPermission());
    menu.setType(resources.getType());
    menuRepository.save(menu);
    // 计算父级菜单节点数目
    updateSubCnt(oldPid);
    updateSubCnt(newPid);
    // 清理缓存
    delCaches(resources.getId());
  }

  @Override
  public Set<Menu> getChildMenus(List<Menu> menuList, Set<Menu> menuSet) {
    for (Menu menu : menuList) {
      menuSet.add(menu);
      List<Menu> menus = menuRepository.findByPid(menu.getId());
      if (menus != null && menus.size() != 0) {
        getChildMenus(menus, menuSet);
      }
    }
    return menuSet;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Set<Menu> menuSet) {
    for (Menu menu : menuSet) {
      // 清理缓存
      delCaches(menu.getId());
      roleService.untiedMenu(menu.getId());
      menuRepository.deleteById(menu.getId());
      updateSubCnt(menu.getPid());
    }
  }

  @Override
  public List<MenuDto> getMenus(Long pid) {
    List<Menu> menus;
    if (pid != null && !pid.equals(0L)) {
      menus = menuRepository.findByPid(pid);
    } else {
      menus = menuRepository.findByPidIsNull();
    }
    return menuMapper.toDto(menus);
  }

  @Override
  public List<MenuDto> getSuperior(MenuDto menuDto, List<Menu> menus) {
    if (menuDto.getPid() == null) {
      menus.addAll(menuRepository.findByPidIsNull());
      return menuMapper.toDto(menus);
    }
    menus.addAll(menuRepository.findByPid(menuDto.getPid()));
    return getSuperior(findById(menuDto.getPid()), menus);
  }

  @Override
  public List<MenuDto> buildTree(List<MenuDto> menuDtos) {
    List<MenuDto> trees = new ArrayList<>();
    Set<Long> ids = new HashSet<>();
    for (MenuDto menuDTO : menuDtos) {
      if (menuDTO.getPid() == null) {
        trees.add(menuDTO);
      }
      for (MenuDto it : menuDtos) {
        if (menuDTO.getId().equals(it.getPid())) {
          if (menuDTO.getChildren() == null) {
            menuDTO.setChildren(new ArrayList<>());
          }
          menuDTO.getChildren().add(it);
          ids.add(it.getId());
        }
      }
    }
    if (trees.size() == 0) {
      trees = menuDtos.stream().filter(s -> !ids.contains(s.getId())).collect(Collectors.toList());
    }
    return trees;
  }

  @Override
  public List<MenuVo> buildMenus(List<MenuDto> menuDtos) {
    List<MenuVo> list = new LinkedList<>();
    menuDtos.forEach(
        menuDTO -> {
          if (menuDTO != null) {
            List<MenuDto> menuDtoList = menuDTO.getChildren();
            MenuVo menuVo = new MenuVo();
            menuVo.setName(
                Strings.isNotEmpty(menuDTO.getComponentName())
                    ? menuDTO.getComponentName()
                    : menuDTO.getTitle());
            // 一级目录需要加斜杠，不然会报警告
            menuVo.setPath(menuDTO.getPid() == null ? "/" + menuDTO.getPath() : menuDTO.getPath());
            menuVo.setHidden(menuDTO.getHidden());
            // 如果不是外链
            if (!menuDTO.getIFrame()) {
              if (menuDTO.getPid() == null) {
                menuVo.setComponent(
                    Strings.isEmpty(menuDTO.getComponent()) ? "Layout" : menuDTO.getComponent());
                // 如果不是一级菜单，并且菜单类型为目录，则代表是多级菜单
              } else if (menuDTO.getType() == 0) {
                menuVo.setComponent(
                    Strings.isEmpty(menuDTO.getComponent())
                        ? "ParentView"
                        : menuDTO.getComponent());
              } else if (Strings.isNotEmpty(menuDTO.getComponent())) {
                menuVo.setComponent(menuDTO.getComponent());
              }
            }
            menuVo.setMeta(
                new MenuMetaVo()
                    .setTitle(menuDTO.getTitle())
                    .setIcon(menuDTO.getIcon())
                    .setNoCache(!menuDTO.getCache()));
            if (CollectionUtils.isNotEmpty(menuDtoList)) {
              menuVo.setAlwaysShow(true);
              menuVo.setRedirect("noredirect");
              menuVo.setChildren(buildMenus(menuDtoList));
              // 处理是一级菜单并且没有子菜单的情况
            } else if (menuDTO.getPid() == null) {
              MenuVo menuVo1 = new MenuVo();
              menuVo1.setMeta(menuVo.getMeta());
              // 非外链
              if (!menuDTO.getIFrame()) {
                menuVo1.setPath("index");
                menuVo1.setName(menuVo.getName());
                menuVo1.setComponent(menuVo.getComponent());
              } else {
                menuVo1.setPath(menuDTO.getPath());
              }
              menuVo.setName(null);
              menuVo.setMeta(null);
              menuVo.setComponent("Layout");
              List<MenuVo> list1 = new ArrayList<>();
              list1.add(menuVo1);
              menuVo.setChildren(list1);
            }
            list.add(menuVo);
          }
        });
    return list;
  }

  @Override
  public Menu findOne(Long id) {
    Menu menu = menuRepository.findById(id).orElseGet(Menu::new);
    ValidationUtil.isNull(menu.getId(), "Menu", "id", id);
    return menu;
  }

  @Override
  public void download(List<MenuDto> menuDtos, HttpServletResponse response) throws IOException {
    List<Map<String, Object>> list = new ArrayList<>();
    for (MenuDto menuDTO : menuDtos) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("菜单标题", menuDTO.getTitle());
      map.put("菜单类型", menuDTO.getType() == null ? "目录" : menuDTO.getType() == 1 ? "菜单" : "按钮");
      map.put("权限标识", menuDTO.getPermission());
      map.put("外链菜单", menuDTO.getIFrame() ? "是" : "否");
      map.put("菜单可见", menuDTO.getHidden() ? "否" : "是");
      map.put("是否缓存", menuDTO.getCache() ? "是" : "否");
      map.put("创建日期", menuDTO.getCreateTime());
      list.add(map);
    }
    FileUtil.downloadExcel(list, response);
  }

  private void updateSubCnt(Long menuId) {
    if (menuId != null) {
      int count = menuRepository.countByPid(menuId);
      menuRepository.updateSubCntById(count, menuId);
    }
  }

  /**
   * 清理缓存
   *
   * @param id 菜单ID
   */
  public void delCaches(Long id) {
    List<User> users = userRepository.findByMenuId(id);
    redisUtils.del(CacheKey.MENU_ID + id);
    redisUtils.delByKeys(
        CacheKey.MENU_USER, users.stream().map(User::getId).collect(Collectors.toSet()));
    // 清除 Role 缓存
    List<Role> roles =
        roleService.findInMenuId(
            new ArrayList<Long>() {
              {
                add(id);
              }
            });
    redisUtils.delByKeys(
        CacheKey.ROLE_ID, roles.stream().map(Role::getId).collect(Collectors.toSet()));
  }
}
