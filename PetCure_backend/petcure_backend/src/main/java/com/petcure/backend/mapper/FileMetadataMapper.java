package com.petcure.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.petcure.backend.entity.FileMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


//这个Mapper操作的是FileMetadata对应的表
//虽然接口内部没有手写方法，但已经继承了常用操作：insert(...),selectById(...),selectCount(...)
@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {
    //给文件Mapper添加归属检查和锁定查询

    //Select声明查询SQL，MyBatis负责执行，并把结果映射成USER或FileMetadata对象。这两个方法都不需要另写XML
    //后面会在Service层中依次锁定用户，锁定文件，再更新用户头像和文件状态.事物提交或回滚后，锁会释放
    @Select("""
            SELECT *
            FROM `file`
            WHERE id = #{fileId}
                AND user_id = #{userId}
            FOR UPDATE
            """)    //含义是查找这个文件而且它必须属于当前登录用户

    FileMetadata selectOwnedFileForUpdate(
            @Param("fileId")Long fileId,
            @Param("userId")Long userId
    );
    //添加归属检查和锁定查询
    //文件存在，且属于当前用户，返回文件记录
    //文件不存在，返回null
    //文件属于其它用户，返回null
    //后面的service可以统一提示“文件头像不可用”,不向用户区分文件不存在还是属于别人
    //文件记录也要锁定，因为关联状态即将从“临时”变成“已关联”，这个过程要与后续稳健清理逻辑协调

    @Select("""
            SELECT f.*
            FROM `file` f
            INNER JOIN `USER` u ON u.id = f.user_id
            WHERE f.id = #{fileId}
            AND f.user_id = #{userId}
            AND f.status = 1
            AND u.avatar_url = CONCAT('/api/files/',f.id,'/content') 
            """)
    FileMetadata selectCurrentAvatarForRead(
            @Param("fileId")Long fileId,
            @Param("userId")Long userId
    );

    //它负责查询“属于当前用户，而且仍然被用作当前头像”的文件



    //添加引用数量查询
    //用于处理更换头像后的旧文件状态：
    //绑定新头像->检查旧文件是否还有用户头像引用->没有引用：标记为2->下一步由定时任务删除
    //这一轮会完成Mapper查询、旧文件标记和现有保存方法接入

    @Select("""
            SELECT COUNT(*)
            FROM `USER`
            WHERE avatar_url = CONCAT('/api/files/',#{fileId},'/content')
            """)

    long counterUserAvatarReferences(@Param("fileId")long fileId);
    //它检查这个文件地址是否仍然出现在用户的avatar_url中
    //目前项目只有用户头像引用文件，因此先检查USER表。以后接入宠物头像或附件时，需要把对应的引用检查一起扩展



    //添加清理候选查询

    //每轮最多选择100条头像文件记录
    //这个方法负责找候选ID
    @Select("""
            SELECT id
            FROM `file`
            WHERE storage_key LIKE 'avatars/%'
                AND (
                    status = 2
                    OR(
                        status = 0
                        AND created_at < UTC_TIMESTAMP()-INTERVAL 24 HOUR
                    )
                )
                ORDER BY id
                LIMIT 100
            """)
    List<Long>selectAvatarCleanupCandidates();





    //真正处理之前，再次检查条件并锁定记录
    //第二个方法在事务中重新确认状态，因为扫描之后处理之前，文件可能已经被用户关联
    @Select("""
            SELECT *
            FROM `file`
            WHERE id = #{fileId}
                AND storage_key LIKE 'avatars/%'
                AND (
                    status = 2
                    OR(
                    status = 0
                    AND created_at < UTC_TIMESTAMP() - INTERVAL 24 HOUR
               )
                )
            """)
    FileMetadata selectAvatarCleanupCandidateForUpdate(
            @Param("fileId")Long fileId
    );

    //第一次查到的结果，只能代表查询当时的状态。等程序开始处理某个文件时，它的状态可能已经变了
    //例如文件2是一个超过24小时的临时文件：
    //清理任务扫描，得到候选ID：[1, 2, 3]，此时文件2的 status = 0
    //用户刚好将文件2设置为头像， 文件2的 status 变成1
    //清理任务开始处理文件2，执行第二个查询，status = 1，不再满足清理条件，查询返回null，跳过这个文件

    //也就是说如果只根据第一次得到的ID直接删除，就可能把用户刚设置好的头像删掉。
    //第二个查询为什么要加锁
    //重新检查条件后，到实际删除之间，也存在时间间隔。
    //FOR UPDATE 会在事务中锁定这条文件记录，让其他需要修改它的事务等待，直到本次事务提交或回滚。这样可以将：
    //重新检查文件状态->检查是否还有头像引用->删除磁盘文件->删除数据库记录
    //放在受锁保护的处理过程中。我们设置头像时也使用文件锁，因此两种操作会协调执行。
    //而且第二个需要取得 storage_key，才能找到磁盘文件，还需要其他元数据

}
