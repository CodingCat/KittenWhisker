package me.codingcat.happysparking;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.sun.tools.attach.VirtualMachine;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class PerfAgent {

  private static String perfDataFilePath;
  private static String symbolFilePath;

  private static boolean hasResource(String path) {
    return PerfAgent.class.getResource(path) != null;
  }

  private static File extractLibraryFile(
      String libraryPath,
      String libraryFileName,
      String targetFolder) {

    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("happysparking-%s-%s", uuid, libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);

    try {
      // Extract a native library file into the target directory
      InputStream reader = null;
      FileOutputStream writer = null;
      try {
        reader = PerfAgent.class.getResourceAsStream(libraryPath + libraryFileName);
        try {
          writer = new FileOutputStream(extractedLibFile);

          byte[] buffer = new byte[8192];
          int bytesRead = 0;
          while ((bytesRead = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, bytesRead);
          }
        } finally {
          if (writer != null) {
            writer.close();
          }
        }
      } finally {
        if (reader != null) {
          reader.close();
        }

        // Delete the extracted lib file on JVM exit.
        extractedLibFile.deleteOnExit();
      }

      // Set executable (x) flag to enable Java to load the native library
      boolean success = extractedLibFile.setReadable(true) &&
              extractedLibFile.setWritable(true, true) &&
              extractedLibFile.setExecutable(true);
      if (!success) {
        // Setting file flag may fail, but in this case another error will be thrown in later phase
      }

      // Check whether the contents are properly copied from the resource folder
      {
        InputStream nativeIn = null;
        InputStream extractedLibIn = null;
        try {
          nativeIn = PerfAgent.class.getResourceAsStream(libraryPath + libraryFileName);
          extractedLibIn = new FileInputStream(extractedLibFile);

          if (!contentsEquals(nativeIn, extractedLibIn)) {
            throw new RuntimeException(String.format("Failed to write a native library file at %s", extractedLibFile));
          }
        } finally {
          if (nativeIn != null) {
            nativeIn.close();
          }
          if (extractedLibIn != null) {
            extractedLibIn.close();
          }
        }
      }

      return new File(targetFolder, extractedLibFileName);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return null;
    }
  }

  private static boolean contentsEquals(InputStream in1, InputStream in2)
          throws IOException {
    if (!(in1 instanceof BufferedInputStream)) {
      in1 = new BufferedInputStream(in1);
    }
    if (!(in2 instanceof BufferedInputStream)) {
      in2 = new BufferedInputStream(in2);
    }

    int ch = in1.read();
    while (ch != -1) {
      int ch2 = in2.read();
      if (ch != ch2) {
        return false;
      }
      ch = in1.read();
    }
    int ch2 = in2.read();
    return ch2 == -1;
  }

  private static File findNativeLibrary() throws IOException {
    // Load native library inside a jar file
    String perfjNativeLibraryPath = "/";
    String perfjNativeLibraryName = "libperfmap.so";
    boolean hasNativeLib = hasResource(perfjNativeLibraryPath + perfjNativeLibraryName);

    if (!hasNativeLib) {
      throw new RuntimeException("no native library is found for happysparking");
    }

    File tempFolder = new File(System.getProperty("java.io.tmpdir"));
    if (!tempFolder.exists()) {
      boolean created = tempFolder.mkdirs();
      if (!created) {
        throw new IOException("cannot create tmp directory");
      }
    }
    // Extract and load a native library inside the jar file
    return extractLibraryFile(perfjNativeLibraryPath, perfjNativeLibraryName,
            tempFolder.getAbsolutePath());
  }

  // TODO: for running after VM start
  public static void agentmain(final String args, final Instrumentation instrumentation) {
    // int passedInPID = Integer.valueOf(args[0])
  }

  private static HashMap<String, Object> parseArgs(String args) {
    HashMap<String, Object> m = new HashMap<>();
    String[] argumentArray = args.split(",");
    for (String arg: argumentArray) {
      String[] kvPair = arg.split("=");
      String key = kvPair[0];
      switch (key) {
        case "waitingLength":
          m.put(key, Integer.valueOf(kvPair[1]));
          break;
        case "targetDirectory":
          m.put(key, kvPair[1]);
          break;
        default:
          if (!m.containsKey("options")) {
            m.put("options", "");
          }
          m.put("options", m.get("options") + "," + key);
          break;
      }
    }
    return m;
  }

  private static boolean uploadFileToSharedDirectory(String source, String targetDirectory) {
    try {
      FileSystem fs = FileSystem.get(new Configuration());
      Path targetDir = new Path(targetDirectory);
      if (!fs.exists(targetDir)) {
        boolean mkdirResult = fs.mkdirs(targetDir);
        if (!mkdirResult) {
          throw new IOException("cannot create directory " + targetDirectory);
        }
      }
      fs.copyFromLocalFile(new Path(source), targetDir);
      return true;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
  }

  private static void moveGeneratedFileToCWD(int pid) {
    String generatedPath = "/tmp/perf-" + pid + ".map";
    String targetPath = System.getProperty("java.io.tmpdir") + "/perf-" + pid + ".map";
    try {
      File generatedFilePath = new File(generatedPath);
      Files.move(generatedFilePath.toPath(), new File(targetPath).toPath(),
              StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ioe){
      ioe.printStackTrace();
      File f = new File(generatedPath);
      if (f.exists()) {
        f.delete();
      }
    }
  }

  private static String getPerfParams(int pid) {
    try {
      FileInputStream perfConfFile = new FileInputStream("./perf.conf");
      BufferedReader br = new BufferedReader(new InputStreamReader(perfConfFile));
      String parameters = br.readLine();
      assert (parameters != null);
      return parameters;
    } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void startPerf(int pid) {
    // 1. read the file containing the parameters for building parameters
    try {
      String parameters = getPerfParams(pid);
      assert (parameters != null);
      // 2. start the process to start perf program
      List<String> l = new ArrayList<String>();
      for (String str: parameters.split(" ")) {
        l.add(str);
      }
      // add commands about pid and output file
      l.add("-p");
      l.add(String.valueOf(pid));
      l.add("-o");
      l.add(perfDataFilePath);
      ProcessBuilder pb = new ProcessBuilder(l);
      Process proc = pb.start();
      proc.waitFor();
      assert(proc.exitValue() == 0);
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void setGlobalPaths(int pid) {
    perfDataFilePath = System.getProperty("java.io.tmpdir") + "/executor-" +
            String.valueOf(pid) + ".data";
    symbolFilePath = System.getProperty("java.io.tmpdir") + "/perf-" + pid + ".map";
  }

  private static void uploadFiles(String targetDirectory, int currentVMPID) {
    try {
      boolean uploadPerfDataFile = uploadFileToSharedDirectory(perfDataFilePath, targetDirectory);
      if (!uploadPerfDataFile) {
        throw new IOException("cannot upload perf data files for process " + currentVMPID);
      }
      boolean uploadSymbolFile = uploadFileToSharedDirectory(symbolFilePath, targetDirectory);
      if (!uploadSymbolFile) {
        throw new IOException("cannot upload symbol files for process " + currentVMPID);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(1);
    }
  }

  public static void premain(final String args, final Instrumentation instrumentation) {
    try {
      // TODO: use future
      // fork a new process
      new Thread() {
        @Override
        public void run() {
          VirtualMachine vm;
          HashMap<String, Object> argMap = parseArgs(args);
          System.out.println("================finished paring argument===========");
          int waitingLength = (Integer) argMap.get("waitingLength");
          String targetDirectory = (String) argMap.get("targetDirectory");
          String options = (String) argMap.get("options");
          String currentVMPID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
          int pid =  Integer.valueOf(currentVMPID);
          System.out.println("================attaching to " + currentVMPID + "===========");
          try {
            File f = findNativeLibrary();
            System.out.println("================found library =========================");
            setGlobalPaths(pid);
            Thread.sleep(waitingLength);
            // start new process for linux perf
            System.out.println("================starting linux perf =========================");
            startPerf(pid);
            System.out.println("================start generating symbol files ===========");
            vm = VirtualMachine.attach(currentVMPID);
            vm.loadAgentPath(f.getAbsolutePath(), options);
            System.out.println("================DONE===========");
            moveGeneratedFileToCWD(pid);
            uploadFiles(targetDirectory, pid);
          } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
          }
        }
        // upload symbol file

      }.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // TODO： upload the generated file to target directory in shared storage system
  }
}
