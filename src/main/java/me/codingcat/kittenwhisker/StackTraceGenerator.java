package me.codingcat.kittenwhisker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
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

  String generateStackTrace(String localPath, String dataFileName) {
    try {
      ProcessBuilder pb = new ProcessBuilder(
              "perf", "script", "-i",
              localPath + "/" + dataFileName);
      File outputFile = new File(outputStackFileName(dataFileName));
      System.out.println("produce output stack file at " + outputFile.getAbsolutePath());
      pb.redirectOutput(outputFile);
      Process p = pb.start();
      new Thread() {
        public void run() {
          try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = reader.readLine()) != null) {
              System.out.println(line);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }.start();
      p.waitFor();
      if (p.exitValue() != 0) {
         throw new Exception("process returns with " + p.exitValue());
      }
      return outputFile.getAbsolutePath();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }
}
