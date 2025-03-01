package com.owner.shopping_message_service;

import com.owner.shopping_common.result.BaseResult;
import com.owner.shopping_common.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShoppingMessageServiceApplicationTests {
	@Autowired
	private MessageService service;
	@Test
	void contextLoads() {
		BaseResult baseResult = service.sendMessage("13217321169", "1234");
		System.out.println(baseResult);
	}

}
