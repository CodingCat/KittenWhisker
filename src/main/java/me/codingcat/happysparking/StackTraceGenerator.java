package me.codingcat.happysparking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class StackTraceGenerator {

  private List<String> filesToBeCleaned = new ArrayList<>();

  private String getPIDFromFileName(String dataFileName) {
    String[] separatedByDash = dataFileName.split("-");
    String last = separatedByDash[separatedByDash.length - 1];
    System.out.println(last);
    String[] pidAndSuffix = last.split("\\.");
    return pidAndSuffix[0];
  }

  private String deriveSymbolFileName(String dataFileName) {
    int lastDotIndex = dataFileName.lastIndexOf(".");
    String prefix = dataFileName.substring(0, lastDotIndex);
    return prefix + ".map";
  }

  private String outputStackFileName(String dataFileName) {
    int lastDotIndex = dataFileName.lastIndexOf(".");
    String prefix = dataFileName.substring(0, lastDotIndex);
    return prefix + ".stack";
  }

  private void generateStackTrace(String localPath, String dataFileName) {
    try {
      // 1. get the pid and pick up the corresponding map file
      String pid = getPIDFromFileName(dataFileName);
      String symbolFileName = deriveSymbolFileName(dataFileName);
      // 2. move map file to /tmp/pid.map
      Files.copy(new File(localPath + "/" + symbolFileName).toPath(),
              new File("/tmp/" + pid + ".map").toPath(),
              StandardCopyOption.REPLACE_EXISTING);
      filesToBeCleaned.add("/tmp/" + pid + ".map");
      // 3. run perf script
      ProcessBuilder pb = new ProcessBuilder(
              "sudo", "perf", "script", "-i",
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

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          for (String file : filesToBeCleaned) {
            File f = new File(file);
            if (f.exists()) {
              Files.delete(new File(file).toPath());
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  public static void main(String[] args) {
    StackTraceGenerator traceGenerator = new StackTraceGenerator();
    traceGenerator.registerShutdownHook();
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
