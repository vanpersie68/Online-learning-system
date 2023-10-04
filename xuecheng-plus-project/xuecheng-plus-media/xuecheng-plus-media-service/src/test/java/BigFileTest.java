import com.xuecheng.base.model.PageParams;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BigFileTest
{
    //测试分块
    @Test
    public void testChunk() throws IOException
    {
        //1. 找到源文件
        File sourceFile = new File("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day06 断点续传 xxl-job\\Day6-13.视频处理-xxl-job-高级配置参数.mp4");
        //2. 分块文件存储路径
        String chunkFilePath = "E:\\test\\chunk\\";
        //3. 分块文件的大小
        int chunkSize = 1024 * 1024 * 5; //5MB
        //4. 分块文件的个数
        int chunkNum = (int) Math.ceil(sourceFile.length() * 1.0 / chunkSize); //Math.ceil 向上取整
        //使用流从源文件中读数据，像分块文件中写数据
        RandomAccessFile r = new RandomAccessFile(sourceFile, "r");
        //缓存区
        byte[] bytes = new byte[1024];

        for (int i = 0; chunkNum > i; i++)
        {
            File tempFile = new File(chunkFilePath + i);
            //分块文件的写入流
            RandomAccessFile rw = new RandomAccessFile(tempFile, "rw");
            int len = -1;
            while((len = r.read(bytes)) != -1)
            {
                rw.write(bytes, 0, len);
                if(tempFile.length() >= chunkSize)
                    break;
            }

            rw.close();
        }

        r.close();
    }

    //测试合并
    @Test
    public void testMerge() throws IOException
    {
        //找到源文件
        File sourceFile = new File("E:\\黑马程序员\\1、黑马程序员Java项目《学成在线》企业级开发实战\\学成在线项目—视频\\day06 断点续传 xxl-job\\Day6-13.视频处理-xxl-job-高级配置参数.mp4");

        //分块文件存储路径
        File chunkFolder = new File("E:\\test\\chunk\\");
        //合并后的文件
        File mergeFile = new File("E:\\test\\a1.mp4");

        //取出所有的分块文件
        File[] files = chunkFolder.listFiles();
        List<File> fileList = Arrays.asList(files);
        //对文件进行排序，否则会乱序
        Collections.sort(fileList, new Comparator<File>()
        {
            @Override
            public int compare(File o1, File o2)
            {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });

        //向合并的文件写的流
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];

        //遍历分块文件，向合并的文件写
        for (File file : fileList)
        {
            //读分块的流
            RandomAccessFile r = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = r.read(bytes)) != -1)
            {
                rw.write(bytes, 0, len);
            }

            r.close();
        }

        rw.close();

        //合并完成后对文件进行校验
        FileInputStream fileInputStreamMerge = new FileInputStream(mergeFile);
        FileInputStream fileInputStreamSource = new FileInputStream(sourceFile);
        String merge = DigestUtils.md5Hex(fileInputStreamMerge);
        String source = DigestUtils.md5Hex(fileInputStreamSource);

        if(merge.equals(source))
        {
            System.err.println("文件合并成功");
        }

    }
}
