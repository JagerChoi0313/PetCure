package com.petcure.backend;

import com.petcure.backend.mapper.USERMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLOutput;


@SpringBootTest
class PetcureBackendApplicationTests {

	@Autowired
	private USERMapper userMapper;

	@Test
	void shouldQueryUserCount() {
			Long count=userMapper.selectCount(null);
			System.out.println("用户表中的记录数量："+count);
	}

}
