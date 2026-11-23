package com.petcure.backend.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.petcure.backend.entity.FileMetadata;
import com.petcure.backend.mapper.FileMetadataMapper;
import com.petcure.backend.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class FileCleanupJob {

    private final FileMetadataMapper fileMetadataMapper;
    private final FileStorageService fileStorageService;
    private final TransactionTemplate transactionTemplate;

    public FileCleanupJob(
            FileMetadataMapper fileMetadataMapper,
            FileStorageService fileStorageService,
            PlatformTransactionManager transactionManager
    ){
        this.fileMetadataMapper = fileMetadataMapper;
        this.fileStorageService = fileStorageService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

    }

    //启动一分钟后执行，每轮结束后等待一分钟再执行
    @Scheduled(initialDelay = 60_000,fixedDelay = 60_0000)
    public void cleanupAvatars(){
        List<Long> fileIds =
                fileMetadataMapper.selectAvatarCleanupCandidates();

        for (Long fileId : fileIds){
            try{
                //每个文件分别使用一个事务
                transactionTemplate.executeWithoutResult(
                        transactionStatus -> cleanupOne(fileId)
                );
            }catch (RuntimeException exception){
                log.error(
                        "头像文件清理失败，保留记录等待尝试,fileId={}",
                        fileId,
                        exception
                );
            }
        }
    }


    //是否允许删除，由下面的清理任务先检查数据库的状态和引用
    private void cleanupOne(Long fileId){
        FileMetadata metadata =
                fileMetadataMapper.selectAvatarCleanupCandidateForUpdate(
                        fileId
                );

        //文件已经被清理，或者现在已经不满足清理条件
        if (metadata == null){
            return;
        }

        //仍被用户头像引用，保留文件并修正为已关联
        if (fileMetadataMapper.counterUserAvatarReferences(fileId)>0){
            int updateRows = fileMetadataMapper.update(
                    null,
                    new LambdaUpdateWrapper<FileMetadata>()
                            .eq(FileMetadata::getId,fileId)
                            .in(FileMetadata::getStatus,0,2)
                            .set(FileMetadata::getStatus,1)
            );

            if (updateRows != 1){
                throw new IllegalStateException("修正文件关联状态失败");
            }

            return;
        }

        //没有引用，先删除磁盘文件
        try{
            fileStorageService.deleteAvatarFile(
                    metadata.getStorage_key()
            );
        }catch (IOException exception){
            throw new IllegalStateException(
                    "删除头像磁盘文件失败",
                    exception
            );
        }

        //再删除元数据记录
        int deletedRows = fileMetadataMapper.deleteById(fileId);

        if (deletedRows != 1){
            throw new IllegalStateException("删除文件元数据失败");
        }
    }
}
