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
		//创建分词请求
		AnalyzeRequest request = AnalyzeRequest.of(a -> a.index("goods_index")
				.text(text)
				.analyzer("ik_pinyin_analyzer"));
		//发送分词请求
		AnalyzeResponse response = client.indices()
				.analyze(request);
		//处理分词请求
		List<AnalyzeToken> tokens = response.tokens();

		List<String> words = new ArrayList<>();
		tokens.forEach(token -> words.add(token.token()));

		return words;
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
			MultiMatchQuery query = MultiMatchQuery.of(q -> q.query(keyword)
					.fields("goodsName", "caption", "brand"));
			builder.must(query._toQuery());
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
				//查询集合或者对象，其域field为集合或对象名.属性.搜索条件关键字
				TermQuery specificationQuery = TermQuery.of(q -> q.field("specification." + key + ".keyword")
						.value(value));
				builder.must(specificationQuery._toQuery());
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
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "id");
				}
				if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "id");
				}
			}
			if (sortFiled.equals("PRICE")) {
				if (sort.equals("ASC")) {
					sortParam = Sort.by(Sort.Direction.ASC, "price");
				}
				if (sort.equals("DESC")) {
					sortParam = Sort.by(Sort.Direction.DESC, "price");
				}
			}
			nativeQueryBuilder.withSort(sortParam);

		}
		//8 返回查询条件对象
		return nativeQueryBuilder.build();//Builder.build生成对应的query对象
	}

	@Override
	public void SyncGoodsToES(GoodsDesc goodsDesc) {
		try {
			//将商品详情对象转换成GoodsES对象
			//商品基本属性
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

			//类型集合
			List<String> list = new ArrayList<>();
			list.add(goodsDesc.getProductType1().getName());
			list.add(goodsDesc.getProductType2().getName());
			list.add(goodsDesc.getProductType3().getName());
			goodsES.setProductType(list);

			//商品规格集合
			Map<String, List<String>> map = new HashMap<>();
			//遍历商品规格，封装成goodses所需
			List<Specification> specifications = goodsDesc.getSpecifications();
			for (Specification specification : specifications) {
				//获取规格项
				List<SpecificationOption> options = specification.getSpecificationOptions();
				//获取规格项名
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
			
			//关键词
			List<String> tags = new ArrayList<>();
			tags.add(goodsDesc.getBrand().getName());//品牌名为关键词
			tags.addAll(analyze(goodsName));//商品名分词后为关键词
			tags.addAll(analyze(caption));//商品副标题分词后为关键词
			log.info("关键词:{}",tags);
			goodsES.setTags(tags);
			
			//将GoodsES对象存入ES
			log.info("商品详情:{}",goodsES);
			repository.save(goodsES);

		} catch (Exception e) {
			log.error("同步商品到ES异常，ID: {}, 错误: {}", goodsDesc.getId(), e.getMessage());
		}
	}

	// 清理文本中的特殊字符
	private String cleanText(String text) {
		if (text == null) {
			return "";
		}
		
		// 替换特殊字符
		return text.replace("\"", " ")
				  .replace("\\", " ")
				  .replace("\n", " ")
				  .replace("\r", " ")
				  .replace("\t", " ")
				  .replaceAll("[\\p{Cc}\\p{Cf}]", "")
				  .replaceAll("\\s+", " ")
				  .trim();
	}

	//每天凌晨3点同步到数据库
	// @Scheduled(cron = "0 0 3 * * ? ")
	@Override
	public void ScheduledSyncToES() {
		log.info("开始同步商品数据goods到ES搜索引擎goodsDesc");
		List<GoodsDesc> all = goodsService.findAllDesc();
		log.info("商品详情list:{}",all);
		all.forEach(this::SyncGoodsToES);
		log.info("同步商品数据goods到ES搜索引擎goodsDesc结束");
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
