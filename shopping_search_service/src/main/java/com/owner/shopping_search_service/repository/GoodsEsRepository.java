package com.owner.shopping_search_service.repository;

import com.owner.shopping_common.pojo.GoodsES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * GoodsES:实体类
 * Long :主键类型
 */
@Repository
public interface GoodsEsRepository extends ElasticsearchRepository<GoodsES,Long> {
}
