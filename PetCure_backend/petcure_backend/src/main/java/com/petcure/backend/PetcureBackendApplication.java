package com.petcure.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling		//启用定时任务，保存并重新编译、启动1后，任务就会自动运行
@SpringBootApplication
public class PetcureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetcureBackendApplication.class, args);
	}

}

//当前规则是
//文件情况						处理
//状态1，正常关联         		不列入清理候选
//状态0，不满24小时				保留
//状态0，超过24小时且无引用		删除文件和记录
//状态2，且无引用				删除文件和记录
//候选文件实际仍被引用			保留，修正为状态1
//单条处理失败				保留记录，下轮重试
