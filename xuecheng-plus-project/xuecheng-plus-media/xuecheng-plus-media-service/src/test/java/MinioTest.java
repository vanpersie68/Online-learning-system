import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class MinioTest
{
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.101.65:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void testUpload()
    {
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE; //通用mimeType，字节流
        if(extensionMatch != null)
        {
            mimeType = extensionMatch.getMimeType();
        }

        try
        {
            UploadObjectArgs testBucket = UploadObjectArgs.builder()
                    .bucket("testbucket") //桶
                    .filename("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day01 项目介绍&环境搭建\\Day1-00.项目导学.mp4") //指定本地文件的路径
                    .object("001/1.mp4") //对象名
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(testBucket);
            System.out.println("上传成功");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("上传失败");
        }
    }

    @Test
    public void delete()
    {
        try
        {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket("testbucket")
                    .object("1.mp4")
                    .build());
            System.out.println("删除成功");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("删除失败");
        }
    }

    //查询且下载文件
    @Test
    public void getFile() throws Exception
    {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("001/1.mp4")
                .build();

            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            FileOutputStream outputStream = new FileOutputStream(
                    new File("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day05 媒资管理 Nacos Gateway MinIO\\1.mp4"));

            IOUtils.copy(inputStream,outputStream);

            //校验文件的完整性,对文件的内容进行md5校验
            String source_md5 = DigestUtils.md5Hex(new FileInputStream(new File("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day01 项目介绍&环境搭建\\Day1-00.项目导学.mp4")));
            String local_md5 = DigestUtils.md5Hex(new FileInputStream(new File("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day05 媒资管理 Nacos Gateway MinIO\\1.mp4")));

            if(source_md5.equals(local_md5))
            {
                System.out.println("下载成功");
            }
    }

    //将分块文件上传到Minio
    @Test
    public void uploadChunk() throws Exception
    {
        for (int i = 0; i < 8; i++)
        {
            UploadObjectArgs testBucket = UploadObjectArgs.builder()
                    .bucket("testbucket") //桶
                    .filename("E:\\test\\chunk\\" + i) //指定本地文件的路径
                    .object("chunk/" + i) //对象名
                    .build();
            minioClient.uploadObject(testBucket);
            System.out.println("上传分块" + i + "成功");
        }
    }

    //调用minio接口合并分块
    @Test
    public void testMerge() throws Exception
    {
        /*List<ComposeSource> sourceList = new ArrayList<>();
        for (int i = 0; i < 37; i++)
        {
            //指定分块文件的信息
            ComposeSource composeSource = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
            sourceList.add(composeSource);
        }*/

        List<ComposeSource> sourceList = Stream.iterate(0, i -> ++i).limit(8)
                .map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build()).collect(Collectors.toList());

        //指定合并后的objectName等信息
        ComposeObjectArgs testbucket = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sourceList) //指定源文件
                .build();
        //合并文件
        minioClient.composeObject(testbucket);
    }

    //批量清理分块文件
}
