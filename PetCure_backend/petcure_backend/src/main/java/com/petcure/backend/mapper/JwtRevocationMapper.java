package com.petcure.backend.mapper;
//这段代码是一个JWT令牌注销记录（Token黑名单）的数据库操作接口
//作用为：当用户退出登录时，把当前JWT令牌的信息保存到数据库中。
// 以后用户再使用这个令牌访问后端的时候，后端就可以检查它是否已经被注销，从而阻止用户继续使用它的令牌

//JwtRevocationMapper只完成了把JWT撤销记录写入数据库的能力
//真正退出登录还需要退出登录接口和JWT校验逻辑

//现在我们要实现退出登录，目前我们已经在数据库建好了退出用户的令牌id记录表
//POST /api/auth/logout  会从当前请求已通过校验的JWT中取出令牌编号，把撤销记录写进刚好建好的表
//下一步再让JWT校验读取这张表
//这张表用issuer和jti组成主键。我们直接用一条SQL写入，同一枚令牌重复提交保留原有记录


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;//Mapper是一个Mybatis数据库映射接口
import org.apache.ibatis.annotations.Param;//给SQL参数起名字

import java.time.LocalDateTime;

@Mapper
public interface JwtRevocationMapper {//这里定义的是接口，不是普通的java类，具体执行SQL的工具交给Mybatis执行

    //@Insert 注解（最重要的部分）
    @Insert("""                 
            INSERT INTO jwt_revocation
                (issuer, 
                 jti, 
                 expires_at, 
                 revoked_at)
            VALUES 
                (#{issuer},
                #{jti},
                #{expiresAt},
                UTC_TIMESTAMP())
            ON DUPLICATE KEY UPDATE 
                 revoked_at = jwt_revocation.revoked_at
            """)

    void insertRevocation(
            @Param("issuer") String issuer, //把Java参数与SQL参数对应起来，例如@Param("issuer") String issuer对应#{issuer}
            @Param("jti")String jti,
            @Param("expiresAt")LocalDateTime expiresAt
            );
}

//#{jti} 会绑定到 @Param("jti") 对应的参数。
// `UTC_TIMESTAMP()` 由数据库生成撤销时间。唯一主键和这条 SQL 一起处理重复写入。
