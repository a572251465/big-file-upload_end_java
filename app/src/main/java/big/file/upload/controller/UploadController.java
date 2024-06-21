package big.file.upload.controller;

import big.file.upload.entity.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/upload")
public class UploadController {
  
  // 表示临时目录
  private static final String tmpDir;
  private static final String publicDir;
  
  static {
    tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "tmp";
    publicDir = System.getProperty("java.io.tmpdir") + File.separator + "public";
    
    mkDirHandler(tmpDir);
    mkDirHandler(publicDir);
  }
  
  /**
   * 创建目录事件
   *
   * @param dirPath 目录名称
   * @author lihh
   */
  public static void mkDirHandler(String dirPath) {
    File dirPathFile = new File(dirPath);
    
    // 判断目录是否存在
    if (!(dirPathFile.exists() && dirPathFile.isDirectory()))
      dirPathFile.mkdir();
  }
  
  /**
   * 写文件的事件
   *
   * @param outputFilePath 输出的文件地址
   * @param inputStream    输入流
   * @author lihh
   */
  public void writeFileHandler(String outputFilePath, InputStream inputStream) {
    try {
      OutputStream outputStream = Files.newOutputStream(Paths.get(outputFilePath));
      
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, length);
      }
      
      outputStream.close();
      inputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * 切片文件上传
   *
   * @param file     上传的文件
   * @param baseDir  基础目录
   * @param fileName 文件名称
   * @author lihh
   */
  @PostMapping("/section/{baseDir}/{fileName}")
  public ResponseEntity upload(MultipartFile file, @PathVariable("baseDir") String baseDir, @PathVariable("fileName") String fileName) throws IOException {
    baseDir = tmpDir + File.separator + baseDir;
    
    // 创建基础目录
    mkDirHandler(baseDir);
    // 表示 文件地址
    String filePath = baseDir + File.separator + fileName;
    
    // 将 文件 写入到磁盘中
    this.writeFileHandler(filePath, file.getInputStream());
    return ResponseEntity.builder().success(true).build();
  }
  
  /**
   * 文件合并中
   *
   * @param baseDir 基础目录
   * @author lihh
   */
  @PostMapping("/merge/{baseDir}")
  public ResponseEntity merge(@PathVariable("baseDir") String baseDir) {
    baseDir = tmpDir + File.separator + baseDir;
    // 临时 基础目录
    File baseDirFile = new File(baseDir);
    
    // 读取目录
    File[] files = baseDirFile.listFiles();
    // 判断目录是否为空
    if (files == null || files.length == 0)
      return ResponseEntity.builder().build();
    
    // 为了防止 这里进行强制排序
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
      
      }
    });
  }
}
