package com.petcure.backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.petcure.backend.entity.USER;
import com.petcure.backend.mapper.USERMapper;
import org.apache.catalina.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.sql.SQLOutput;
import java.util.List;


@SpringBootTest
class PetcureBackendApplicationTests {

	@Autowired
	private USERMapper userMapper;

	@Test
	void shouldQueryUserTable() {
		List<USER> users = userMapper.selectList(
				new LambdaQueryWrapper<USER>().last("LIMIT 1")
		);

		assertNotNull(users);
		System.out.println("用户表字段查询成功");

	}

}
