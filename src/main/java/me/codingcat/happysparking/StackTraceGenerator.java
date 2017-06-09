package me.codingcat.happysparking;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class StackTraceGenerator {

  private String getPIDFromFileName(String dataFileName) {
    String[] separatedByDash = dataFileName.split("-");
    String last = separatedByDash[separatedByDash.length - 1];
    String[] pidAndSuffix = last.split(".");
    return pidAndSuffix[0];
  }

  private String deriveSymbolFileName(String dataFileName) {
    int lastDotIndex = dataFileName.lastIndexOf(".");
    String prefix = dataFileName.substring(0, lastDotIndex + 1);
    return prefix + ".map";
  }

  private String outputStackFileName(String dataFileName) {
    int lastDotIndex = dataFileName.lastIndexOf(".");
    String prefix = dataFileName.substring(0, lastDotIndex + 1);
    return prefix + ".stack";
  }

  private void generateStackTrace(String localPath, String dataFileName) {
    try {
      // 1. get the pid and pick up the corresponding map file
      String pid = getPIDFromFileName(dataFileName);
      String symbolFileName = deriveSymbolFileName(dataFileName);
      // 2. move map file to /tmp/pid.map
      Files.move(new File(localPath + "/" + symbolFileName).toPath(),
              new File("/tmp/" + pid + ".map").toPath(),
              StandardCopyOption.REPLACE_EXISTING);
      // 3. run perf script
      ProcessBuilder pb = new ProcessBuilder(
              "sudo", "perf", "script", "-i",
              localPath + "/" + dataFileName, ">", localPath + "/" + outputStackFileName(dataFileName));
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

  public static void main(String[] args) {
    StackTraceGenerator traceGenerator = new StackTraceGenerator();
    String localDirectory = args[0];
    File localDir = new File(localDirectory);
    if (localDir.isDirectory()) {
      File[] allPerfDataFiles = localDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".data");
        }
      });
      if (allPerfDataFiles != null) {
        for (File file : allPerfDataFiles) {
          traceGenerator.generateStackTrace(localDirectory, file.getName());
        }
      }
    } else {
      System.out.println("FAULT: the local path must be a file");
      System.exit(1);
    }
  }
}
