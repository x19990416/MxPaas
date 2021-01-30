/** create by Guo Limin on 2021/1/30. */
package com.github.x19990416.mxpaas.admin.modules.system.service.impl;

import com.github.x19990416.mxpaas.admin.common.utils.*;
import com.github.x19990416.mxpaas.admin.modules.system.domain.Dict;
import com.github.x19990416.mxpaas.admin.modules.system.domain.DictDetail;
import com.github.x19990416.mxpaas.admin.modules.system.repository.DictDetailRepository;
import com.github.x19990416.mxpaas.admin.modules.system.repository.DictRepository;
import com.github.x19990416.mxpaas.admin.modules.system.service.DictDetailService;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictDetailDto;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictDetailMapper;
import com.github.x19990416.mxpaas.admin.modules.system.service.dto.DictDetailQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "dict")
public class DictDetailServiceImpl implements DictDetailService {

  private final DictRepository dictRepository;
  private final DictDetailRepository dictDetailRepository;
  private final DictDetailMapper dictDetailMapper;
  private final RedisUtil redisUtils;

  @Override
  public Map<String, Object> queryAll(DictDetailQueryCriteria criteria, Pageable pageable) {
    Page<DictDetail> page =
        dictDetailRepository.findAll(
            (root, criteriaQuery, criteriaBuilder) ->
                QueryHelper.getPredicate(root, criteria, criteriaBuilder),
            pageable);
    return PageUtil.toPage(page.map(dictDetailMapper::toDto));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void create(DictDetail resources) {
    dictDetailRepository.save(resources);
    // 清理缓存
    delCaches(resources);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void update(DictDetail resources) {

    DictDetail dictDetail =
        dictDetailRepository.findById(resources.getId()).orElseGet(DictDetail::new);
    ValidationUtil.isNull(dictDetail.getId(), "DictDetail", "id", resources.getId());
    resources.setId(dictDetail.getId());
    dictDetailRepository.save(resources);
    // 清理缓存
    delCaches(resources);
  }

  @Override
  @Cacheable(key = "'name:' + #p0")
  public List<DictDetailDto> getDictByName(String name) {
    return dictDetailMapper.toDto(dictDetailRepository.findByDictName(name));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    DictDetail dictDetail = dictDetailRepository.findById(id).orElseGet(DictDetail::new);
    // 清理缓存
    delCaches(dictDetail);
    dictDetailRepository.deleteById(id);
  }

  public void delCaches(DictDetail dictDetail) {
    Dict dict = dictRepository.findById(dictDetail.getDict().getId()).orElseGet(Dict::new);
    redisUtils.del(CacheKey.DICT_NAME + dict.getName());
  }
}