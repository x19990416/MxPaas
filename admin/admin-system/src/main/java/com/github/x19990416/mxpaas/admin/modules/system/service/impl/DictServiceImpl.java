/** create by Guo Limin on 2021/1/30. */
package com.github.x19990416.mxpaas.admin.modules.system.service.impl;

import com.github.x19990416.mxpaas.admin.common.utils.*;
import com.github.x19990416.mxpaas.admin.modules.system.domain.Dict;
import com.github.x19990416.mxpaas.admin.modules.system.repository.DictRepository;
import com.github.x19990416.mxpaas.admin.modules.system.service.DictService;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictDetailDto;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictDto;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictMapper;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "dict")
public class DictServiceImpl implements DictService {

  private final DictRepository dictRepository;
  private final DictMapper dictMapper;
  private final RedisUtil redisUtils;

  @Override
  public Map<String, Object> queryAll(DictQueryCriteria dict, Pageable pageable) {
    Page<Dict> page =
        dictRepository.findAll(
            (root, query, cb) -> QueryHelper.getPredicate(root, dict, cb), pageable);
    return PageUtil.toPage(page.map(dictMapper::toDto));
  }

  @Override
  public List<DictDto> queryAll(DictQueryCriteria dict) {
    List<Dict> list =
        dictRepository.findAll((root, query, cb) -> QueryHelper.getPredicate(root, dict, cb));
    return dictMapper.toDto(list);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void create(Dict resources) {
    dictRepository.save(resources);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void update(Dict resources) {
    // 清理缓存
    delCaches(resources);
    Dict dict = dictRepository.findById(resources.getId()).orElseGet(Dict::new);
    ValidationUtil.isNull(dict.getId(), "Dict", "id", resources.getId());
    resources.setId(dict.getId());
    dictRepository.save(resources);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Set<Long> ids) {
    // 清理缓存
    List<Dict> dicts = dictRepository.findByIdIn(ids);
    for (Dict dict : dicts) {
      delCaches(dict);
    }
    dictRepository.deleteByIdIn(ids);
  }

  @Override
  public void download(List<DictDto> dictDtos, HttpServletResponse response) throws IOException {
    List<Map<String, Object>> list = new ArrayList<>();
    for (DictDto dictDTO : dictDtos) {
      if (CollectionUtils.isNotEmpty(dictDTO.getDictDetails())) {
        for (DictDetailDto dictDetail : dictDTO.getDictDetails()) {
          Map<String, Object> map = new LinkedHashMap<>();
          map.put("字典名称", dictDTO.getName());
          map.put("字典描述", dictDTO.getDescription());
          map.put("字典标签", dictDetail.getLabel());
          map.put("字典值", dictDetail.getValue());
          map.put("创建日期", dictDetail.getCreateTime());
          list.add(map);
        }
      } else {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("字典名称", dictDTO.getName());
        map.put("字典描述", dictDTO.getDescription());
        map.put("字典标签", null);
        map.put("字典值", null);
        map.put("创建日期", dictDTO.getCreateTime());
        list.add(map);
      }
    }
    FileUtil.downloadExcel(list, response);
  }

  public void delCaches(Dict dict) {
    redisUtils.del(CacheKey.DICT_NAME + dict.getName());
  }
}
