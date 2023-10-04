package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService
{
   /**
    * @description 媒资文件查询方法
    * @param pageParams 分页参数
    * @param queryMediaParamsDto 查询条件
    * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
    * @author Mr.M
    * @date 2022/9/10 8:57
   */
   PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

   /**
    * 上传文件
    * @param companyId 机构id
    * @param uploadFileParamsDto 文件信息
    * @param localFilePath 文件本地路径
    * @param objectName 如果传入了objectName。要按照objectName的目录去存储，如果不传则按年月日去存储
    * @return 文件所有信息
    */
   UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath, String objectName);


   MediaFiles addMediaFilesToDatabase(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName);

   /**
    * @description 检查文件是否存在
    * @param fileMd5 文件的md5
    * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
    */
   public RestResponse<Boolean> checkFile(String fileMd5);

   /**
    * @description 检查分块是否存在
    * @param fileMd5  文件的md5
    * @param chunkIndex  分块序号
    * @return com.xuecheng.base.model.RestResponse<java.lang.Boolean> false不存在，true存在
    */
   public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex);

   /**
    * @description 上传分块
    * @param fileMd5  文件md5
    * @param chunk  分块序号
    * @param localChunkFilePath  本地文件路径
    * @return com.xuecheng.base.model.RestResponse
    */
   public RestResponse uploadChunk(String fileMd5,int chunk,String localChunkFilePath);

   /**
    * @description 合并分块
    * @param companyId  机构id
    * @param fileMd5  文件md5
    * @param chunkTotal 分块总和
    * @param uploadFileParamsDto 文件信息
    * @return com.xuecheng.base.model.RestResponse
    */
   public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);


   /**
    * 从minio下载文件
    * @param bucket 桶
    * @param objectName 对象名称
    * @return 下载后的文件
    */
   public File downloadFileFromMinIO(String bucket, String objectName);

   //将文件上传到Minio
   public boolean addMediaFilesToMinio(String bucket, String localFilePath, String objectName, String mimeType);

   //根据媒资id查询文件信息
   MediaFiles getFileById(String mediaId);
}
