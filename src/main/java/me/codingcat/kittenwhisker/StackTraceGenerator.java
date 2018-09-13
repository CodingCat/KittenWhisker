package me.codingcat.kittenwhisker;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class StackTraceGenerator {

  private String outputStackFileName(String dataFileName) {
    int lastDotIndex = dataFileName.lastIndexOf(".");
    String prefix = dataFileName.substring(0, lastDotIndex);
    return prefix + ".stack";
  }

  void generateStackTrace(String localPath, String dataFileName) {
    try {
      ProcessBuilder pb = new ProcessBuilder(
              "perf", "script", "-i",
              localPath + "/" + dataFileName);
      pb.redirectOutput(new File(localPath + "/" + outputStackFileName(dataFileName)));
      Process p = pb.start();
      p.waitFor();
      if (p.exitValue() != 0) {
         throw new Exception("process returns with " + p.exitValue());
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
