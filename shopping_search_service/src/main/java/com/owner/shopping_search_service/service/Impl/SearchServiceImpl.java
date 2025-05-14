package com.owner.shopping_search_service.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.indices.AnalyzeRequest;
import co.elastic.clients.elasticsearch.indices.AnalyzeResponse;
import co.elastic.clients.elasticsearch.indices.analyze.AnalyzeToken;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.owner.shopping_common.pojo.*;
import com.owner.shopping_common.service.GoodsService;
import com.owner.shopping_common.service.SearchService;
import com.owner.shopping_search_service.repository.GoodsEsRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@DubboService
public class SearchServiceImpl implements SearchService {
	@Autowired
	private ElasticsearchClient client;

	@Autowired
	private GoodsEsRepository repository;

	@Autowired
	private ElasticsearchTemplate template;

	@DubboReference
	private GoodsService goodsService;



	@SneakyThrows
	public List<String> analyze(String text) {
		if (text == null || text.trim().isEmpty()) {
			return new ArrayList<>();
		}
		
		// 创建分词请求，使用ik_pinyin_analyzer
		AnalyzeRequest ikPinyinRequest = AnalyzeRequest.of(a -> a
				.index("goods_index")
				.text(text)
				.analyzer("ik_pinyin_analyzer"));
		
		// 增加ik_max_word分词器，增加召回率
		AnalyzeRequest ikMaxWordRequest = AnalyzeRequest.of(a -> a
				.index("goods_index")
				.text(text)
				.analyzer("ik_max_word"));
		
		// 发送分词请求
		AnalyzeResponse ikPinyinResponse = client.indices().analyze(ikPinyinRequest);
		AnalyzeResponse ikMaxWordResponse = client.indices().analyze(ikMaxWordRequest);
		
		// 处理分词结果
		Set<String> uniqueWords = new HashSet<>(); // 使用Set去重
		
		// 添加ik_pinyin_analyzer分词结果
		List<AnalyzeToken> ikPinyinTokens = ikPinyinResponse.tokens();
		if (ikPinyinTokens != null) {
			ikPinyinTokens.forEach(token -> uniqueWords.add(token.token()));
		}
		
		// 添加ik_max_word分词结果
		List<AnalyzeToken> ikMaxWordTokens = ikMaxWordResponse.tokens();
		if (ikMaxWordTokens != null) {
			ikMaxWordTokens.forEach(token -> uniqueWords.add(token.token()));
		}
		
		return new ArrayList<>(uniqueWords);
	}

	//自动补齐
	@SneakyThrows
	@Override
	public List<String> autoSuggest(String keyword) {
		//构建自动补齐查询条件
		Suggester suggester = Suggester.of(
				s -> s.suggesters("prefix_suggestion", FieldSuggester.of(
								fs -> fs.completion(
										cs -> cs.skipDuplicates(true)//设置跳过重复项
												.size(10)//设置返回建议项数量
												.field("tags")//指定建议字段为tags关键字字段
								)
						))
						.text(keyword)//设置查询的关键字
		);

		//发起自动补齐请求
		SearchResponse<Map> response = client.search(
				s -> s.index("goods_index")
						.suggest(suggester), Map.class
		);

		//处理响应结果
		Map resultMap = response.suggest();  //获取建议结果映射
		List<Suggestion> suggestionList = (List) resultMap.get("prefix_suggestion"); //获取 "prefix_suggestion" 的建议列表

		Suggestion suggestion = suggestionList.get(0); //获取第一个建议对象
		List<CompletionSuggestOption> resultList = suggestion.completion()
				.options(); //获取建议的选项列表

		List<String> keywords = new ArrayList<>();
		for (CompletionSuggestOption completionSuggestOption : resultList) {
			keywords.add(completionSuggestOption.text());//将建议选项封装成集合
		}

		return keywords;
	}

	@Override
	public GoodsSearchResult search(GoodsSearchParam param) {
		log.info("搜索条件：{}", param);
		//构建ES搜索条件
		NativeQuery nativeQuery = buildQuery(param);
		
		//搜索
		SearchHits<GoodsES> search;
		try {
			search = template.search(nativeQuery, GoodsES.class);
		} catch (Exception e) {
			log.error("搜索过程中发生异常: {}", e.getMessage(), e);
			// 返回空结果
			GoodsSearchResult emptyResult = new GoodsSearchResult();
			emptyResult.setGoodsPage(new Page<>());
			emptyResult.setGoodsSearchParam(param);
			return emptyResult;
		}
		
		//将查询结果封装成MybatisPlus的Page对象
		//将SearchHit对象转为List对象
		List<GoodsES> list = new ArrayList<>();
		search.forEach(goodsESSearchHit -> list.add(goodsESSearchHit.getContent()));
		//将List对象转换成MybatisPlus的Page对象
		Page<GoodsES> page = new Page<>();
		page.setCurrent(param.getPage()) //页码
				.setSize(param.getSize()) // 每页条数
				.setTotal(search.getTotalHits()) //总条数
				.setRecords(list); //结果集
		//封装查询结果
		GoodsSearchResult result = new GoodsSearchResult();
		//1封装商品
		result.setGoodsPage(page);
		//2封装查询参数
		result.setGoodsSearchParam(param);
		GoodsSearchParam goodsSearchParam = new GoodsSearchParam();
		goodsSearchParam.setKeyword(param.getKeyword());
		goodsSearchParam.setBrand(param.getBrand());
		goodsSearchParam.setHighPrice(param.getHighPrice());
		goodsSearchParam.setLowPrice(param.getLowPrice());
		//3封装一个查询面板
		buildSearchPanel(goodsSearchParam, result);
		log.info("Returning result: {}", JSON.toJSONString(result));
		return result;
	}

	/**
	 * 构造搜索条件
	 *
	 * @param param 查询条件对象
	 * @return 封装好的搜索条件
	 */
	private NativeQuery buildQuery(GoodsSearchParam param) {
		//1 创建复杂查询条件对象
		NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder();//条件查询对象
		BoolQuery.Builder builder = new BoolQuery.Builder(); //条件builder，用于将多个条件整合到一个builder


		//2 如果查询条件有关键词，关键词可以匹配商品名，副标题，品牌字段，没有则查询所有
		if (!StringUtils.hasText(param.getKeyword())) {
			MatchAllQuery matchAllQuery = new MatchAllQuery.Builder().build();
			builder.must(matchAllQuery._toQuery());
		} else {
			String keyword = param.getKeyword();
			
			// 改进1: 优化关键词搜索，改用简单的多字段加权查询
			MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(q -> q
					.query(keyword)
					.fields("goodsName^3", "caption^2", "brand^1.5") // 设置字段权重，商品名权重最高
			);
			builder.must(multiMatchQuery._toQuery());
			
			// 改进2: 添加模糊查询，处理可能的拼写错误
			FuzzyQuery fuzzyGoodsNameQuery = FuzzyQuery.of(q -> q
					.field("goodsName")
					.value(keyword)
					.fuzziness("AUTO") // 自动确定编辑距离
					.prefixLength(1) // 前缀长度
			);
			
			FuzzyQuery fuzzyBrandQuery = FuzzyQuery.of(q -> q
					.field("brand")
					.value(keyword)
					.fuzziness("AUTO")
					.prefixLength(1)
			);
			
			// 改进3: 将模糊查询添加为should条件，提高召回率
			builder.should(fuzzyGoodsNameQuery._toQuery());
			builder.should(fuzzyBrandQuery._toQuery());
			
			// 改进4: 使用前缀查询来支持部分匹配
			PrefixQuery prefixQuery = PrefixQuery.of(q -> q
					.field("goodsName")
					.value(keyword.toLowerCase())
			);
			builder.should(prefixQuery._toQuery());
			
			// 设置最小匹配条件，至少一个should条件需要匹配
			builder.minimumShouldMatch("1");
		}
		//3 如果有品牌名，精准查询品牌
		String brand = param.getBrand();
		if (StringUtils.hasText(brand)) {
			TermQuery brandQuery = TermQuery.of(q -> q.field("brand")
					.value(brand));
			builder.must(brandQuery._toQuery());
		}

		//4 如果查询有价格，则匹配价格
		Double highPrice = param.getHighPrice();
		Double lowPrice = param.getLowPrice();
		if (highPrice != null && highPrice != 0) {
			RangeQuery gte = RangeQuery.of(q -> q.field("price")
					.lte(JsonData.of(highPrice)));
			builder.must(gte._toQuery());
		}

		if (lowPrice != null && lowPrice != 0) {
			RangeQuery lte = RangeQuery.of(q -> q.field("price")
					.gte(JsonData.of(lowPrice)));
			builder.must(lte._toQuery());
		}

		//5 如果查询有规格项，则精准匹配规格项
		Map<String, String> specificationOption = param.getSpecificationOption();
		if (!CollectionUtils.isEmpty(specificationOption)) {
			Set<Map.Entry<String, String>> entries = specificationOption.entrySet();
			entries.forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				
				// 创建一个嵌套布尔查询，提高规格项搜索的灵活性
				BoolQuery.Builder specBoolBuilder = new BoolQuery.Builder();
				
				// 保留原有的keyword精确匹配
				TermQuery exactMatchQuery = TermQuery.of(q -> q
						.field("specification." + key + ".keyword")
						.value(value));
				specBoolBuilder.should(exactMatchQuery._toQuery());
				
				// 增加对规格值的模糊匹配
				MatchQuery fuzzyMatchQuery = MatchQuery.of(q -> q
						.field("specification." + key)
						.query(value)
						.fuzziness("AUTO"));
				specBoolBuilder.should(fuzzyMatchQuery._toQuery());
				
				// 要求至少一个条件匹配
				specBoolBuilder.minimumShouldMatch("1");
				
				// 将规格项查询添加到主查询中
				builder.must(specBoolBuilder.build()._toQuery());
			});
		}
		//将前面整合的条件加入nativeQueryBuilder
		nativeQueryBuilder.withQuery(builder.build()
				._toQuery());
		//6 添加分页条件
		PageRequest pageable = PageRequest.of(param.getPage() - 1/*前端页码从1开始*/, param.getSize());
		nativeQueryBuilder.withPageable(pageable);
		//7 如果查询有排序，添加排序条件
		String sortFiled = param.getSortFiled();
		String sort = param.getSort();

		if (StringUtils.hasText(sortFiled) && StringUtils.hasText(sort)) {
			Sort sortParam = null;
			if (sortFiled.equals("NEW")) {
				// 修正NEW排序逻辑，使其更符合语义
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "createTime"); // 使用创建时间而非ID
				} else if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "createTime");
				}
			} else if (sortFiled.equals("PRICE")) {
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "price");
				} else if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "price");
				}
			} else if (sortFiled.equals("SALES")) {
				// 增加销量排序
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "sales");
				} else if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "sales");
				}
			} else if (sortFiled.equals("RATING")) {
				// 增加评分排序
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "rating");
				} else if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "rating");
				}
			}
			
			if (sortParam != null) {
				nativeQueryBuilder.withSort(sortParam);
			}
		}
		//8 返回查询条件对象
		return nativeQueryBuilder.build();//Builder.build生成对应的query对象
	}

	@Override
	public void SyncGoodsToES(GoodsDesc goodsDesc) {
		int maxRetries = 3;
		int retryCount = 0;
		boolean success = false;
		
		while (!success && retryCount < maxRetries) {
			try {
				// 将商品详情对象转换成GoodsES对象
				// 商品基本属性
				GoodsES goodsES = new GoodsES();
				
				// 清理需要分词的字段
				String goodsName = cleanText(goodsDesc.getGoodsName());
				String caption = cleanText(goodsDesc.getCaption());
				
				goodsES.setGoodsName(goodsName);
				goodsES.setId(goodsDesc.getId());
				goodsES.setBrand(goodsDesc.getBrand().getName());
				goodsES.setCaption(caption);
				goodsES.setPrice(goodsDesc.getPrice());
				goodsES.setHeaderPic(goodsDesc.getHeaderPic());

				// 类型集合
				List<String> list = new ArrayList<>();
				list.add(goodsDesc.getProductType1().getName());
				list.add(goodsDesc.getProductType2().getName());
				list.add(goodsDesc.getProductType3().getName());
				goodsES.setProductType(list);

				// 商品规格集合
				Map<String, List<String>> map = new HashMap<>();
				// 遍历商品规格，封装成goodses所需
				List<Specification> specifications = goodsDesc.getSpecifications();
				for (Specification specification : specifications) {
					// 获取规格项
					List<SpecificationOption> options = specification.getSpecificationOptions();
					// 获取规格项名
					List<String> optionStrList = new ArrayList<>();
					for (SpecificationOption option : options) {
						optionStrList.add(cleanText(option.getOptionName()));
					}
					map.put(specification.getSpecName(), optionStrList);
				}
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				goodsES.setSpecification(map);
				goodsES.setRating(goodsDesc.getRating());
				goodsES.setSales(goodsDesc.getSales());
				goodsES.setCreateTime(format.format(goodsDesc.getCreateTime()));
				
				// 关键词强化，增加更多维度的搜索关键词
				Set<String> tagsSet = new HashSet<>(); // 使用Set防止重复
				
				// 1. 添加品牌名为关键词
				tagsSet.add(goodsDesc.getBrand().getName());
				
				// 2. 商品名分词后为关键词
				tagsSet.addAll(analyze(goodsName));
				
				// 3. 商品副标题分词后为关键词
				tagsSet.addAll(analyze(caption));
				
				// 4. 添加类目名作为关键词
				tagsSet.add(goodsDesc.getProductType1().getName());
				tagsSet.add(goodsDesc.getProductType2().getName());
				tagsSet.add(goodsDesc.getProductType3().getName());
				
				// 5. 添加规格选项作为关键词
				for (Specification specification : specifications) {
					for (SpecificationOption option : specification.getSpecificationOptions()) {
						tagsSet.add(cleanText(option.getOptionName()));
					}
				}
				
				List<String> tags = new ArrayList<>(tagsSet);
				log.info("商品 [{}] 的关键词: {}", goodsDesc.getId(), tags);
				goodsES.setTags(tags);
				
				// 将GoodsES对象存入ES
				repository.save(goodsES);
				
				// 标记成功
				success = true;
				log.info("成功同步商品到ES, ID: {}", goodsDesc.getId());
				
			} catch (Exception e) {
				retryCount++;
				log.error("同步商品到ES异常 (尝试 {}/{}), ID: {}, 错误: {}", 
						retryCount, maxRetries, goodsDesc.getId(), e.getMessage());
				
				// 如果不是最后一次尝试，等待一段时间后重试
				if (retryCount < maxRetries) {
					try {
						Thread.sleep(1000 * retryCount); // 指数退避策略
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						log.error("线程中断", ie);
					}
				}
			}
		}
		
		if (!success) {
			log.error("同步商品到ES失败，已达到最大重试次数, ID: {}", goodsDesc.getId());
		}
	}

	// 增强后的文本清理方法
	private String cleanText(String text) {
		if (text == null) {
			return "";
		}
		
		// 第一步：基本清理，去除所有特殊字符
		String cleaned = text.replace("\"", " ")
				  .replace("\\", " ")
				  .replace("\n", " ")
				  .replace("\r", " ")
				  .replace("\t", " ")
				  // 移除HTML标签
				  .replaceAll("<[^>]*>", " ")
				  // 移除控制字符
				  .replaceAll("[\\p{Cc}\\p{Cf}]", "")
				  // 替换多个空格为单个空格
				  .replaceAll("\\s+", " ")
				  .trim();
		
		// 第二步：移除常见的无意义字符（包括英文标点和中文标点）
		cleaned = cleaned.replaceAll("[~`!@#$%^&*()+=|{}':;,.<>/?]", " ") // 英文标点
				  .replaceAll("\\p{Punct}", " ") // 所有标点符号
				  .replaceAll("\\s+", " ")
				  .trim()
				  .toLowerCase(); // 转为小写以提高匹配率
		
		return cleaned;
	}

	//每天凌晨3点同步到数据库
	@Scheduled(cron = "0 0 3 * * ? ")
	@Override
	public void ScheduledSyncToES() {
		log.info("开始同步商品数据goods到ES搜索引擎goodsDesc");
		try {
			List<GoodsDesc> all = goodsService.findAllDesc();
			log.info("获取到商品详情数量: {}", all.size());
			
			int successCount = 0;
			int failCount = 0;
			
			for (GoodsDesc goodsDesc : all) {
				try {
					SyncGoodsToES(goodsDesc);
					successCount++;
				} catch (Exception e) {
					failCount++;
					log.error("同步商品 ID: {} 失败: {}", goodsDesc.getId(), e.getMessage(), e);
				}
			}
			
			log.info("同步商品数据完成，成功: {}，失败: {}", successCount, failCount);
		} catch (Exception e) {
			log.error("同步商品数据过程中发生异常: {}", e.getMessage(), e);
		}
	}

	@Override
	public GoodsDesc findDesc(Long id) {
		return goodsService.findDesc(id);
	}


	/**
	 * 封装查询面板，即根据查询条件，找到对应的商品集合，找到前20个关联度最高的商品进行封装
	 *
	 * @param param  查询条件
	 * @param result 查询结果
	 */
	public void buildSearchPanel(GoodsSearchParam param, GoodsSearchResult result) {
		//封装查询条件
		param.setPage(1);
		param.setSize(20);
		param.setSort(null);
		param.setSortFiled(null);

		NativeQuery nativeQuery = buildQuery(param);//查询结果
		SearchHits<GoodsES> search = template.search(nativeQuery, GoodsES.class);

		List<GoodsES> content = new ArrayList<>();

		search.forEach(searchHit -> content.add(searchHit.getContent()));//遍历SearchHits拿到所有商品信息

		//封装查询面板
		//Brand 品牌
		Set<String> brands = new HashSet<>();
		//ProductType 商品类别
		Set<String> productType = new HashSet<>();
		//specification 规格map，一个规格对应多个规格项
		Map<String, Set<String>> specifications = new HashMap<>();
		for (GoodsES goodsES : content) {
			brands.add(goodsES.getBrand());
			productType.addAll(goodsES.getProductType());
			Set<Map.Entry<String, List<String>>> entries = goodsES.getSpecification()
					.entrySet();
			if (entries != null) {
				entries.forEach(entry -> {
					String key = entry.getKey();
					List<String> value = entry.getValue();
					if (!specifications.containsKey(key)) {
						specifications.put(key, new HashSet<>(value));
					} else {
						specifications.get(key)
								.addAll(value);
					}
				});
			}
		}
		result.setBrands(brands);
		result.setProductType(productType);
		result.setSpecifications(specifications);

	}

	@Override
	public void delete(Long id) {
		repository.deleteById(id);
	}
}
