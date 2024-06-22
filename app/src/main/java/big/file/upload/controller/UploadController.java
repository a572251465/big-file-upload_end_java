package big.file.upload.controller;

import big.file.upload.entity.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.nio.file.Files.newInputStream;

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
  private void writeFileHandler(String outputFilePath, InputStream inputStream) {
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
   * 合并多个文件
   *
   * @param files          文件数组
   * @param outputFilePath 合并文件路径
   * @author lihh
   */
  private boolean mergeFileHandler(File[] files, String outputFilePath) throws IOException {
    // 实例化 出力文件流
    // 使用APPEND选项打开或创建文件，并创建追加模式的输出流
    OutputStream outputStream = Files.newOutputStream(Paths.get(outputFilePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    
    // 从这里 开始遍历输入文件
    for (File sourceFile : files) {
      // 判断文件是否存在
      if (!sourceFile.exists()) return false;
      
      // 这里是 输入流
      InputStream inputStream = newInputStream(sourceFile.toPath());
      
      byte[] buffer = new byte[1024];
      int length;
      while ((length = inputStream.read(buffer)) > 0) {
        outputStream.write(buffer, 0, length);
      }
      outputStream.flush();
      inputStream.close();
    }
    
    if (outputStream != null) outputStream.close();
    
    return true;
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
   * 验证文件 是否存在/ 为了实现秒传
   *
   * @param fileName 文件名称
   * @author lihh
   */
  @GetMapping("/verify/{fileName}")
  public ResponseEntity verify(@PathVariable String fileName) {
    // 表示文件
    File file = new File(publicDir + File.separator + fileName);
    return ResponseEntity.builder().success(file.isFile()).build();
  }
  
  /**
   * 切片文件 合并中
   *
   * @param baseDir  基础目录
   * @param fileName 文件名称
   * @author lihh
   */
  @GetMapping("/merge/{baseDir}/{fileName}")
  public ResponseEntity merge(@PathVariable("baseDir") String baseDir, @PathVariable("fileName") String fileName) throws IOException {
    baseDir = tmpDir + File.separator + baseDir;
    // 临时 基础目录
    File baseDirFile = new File(baseDir);
    
    // 读取目录
    File[] files = baseDirFile.listFiles();
    // 判断目录是否为空
    if (files == null || files.length == 0)
      return ResponseEntity.builder().build();
    
    // 为了防止文件顺序乱了 这里进行强制排序
    Arrays.sort(files, new Comparator<File>() {
      @Override
      public int compare(File o1, File o2) {
        String[] p1Arr = o1.getName().split("-"), p2Arr = o2.getName().split("-");
        int lastP1 = Integer.parseInt(p1Arr[1]), lastP2 = Integer.parseInt(p2Arr[1]);
        return lastP1 - lastP2;
      }
    });
    
    // 表示合并后的目录
    String mergePublicDir = publicDir + File.separator + fileName;
    // 判断是否合并成功
    boolean mergeFlag = mergeFileHandler(files, mergePublicDir);
    return ResponseEntity.builder().success(mergeFlag).build();
  }
}
