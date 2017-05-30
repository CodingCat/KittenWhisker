package me.codingcat.happysparking;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.UUID;

import com.sun.tools.attach.VirtualMachine;

public class PerfAgent {

  public static final String PERFJ_SYSTEM_PROPERTIES_FILE = "info-minzhou-perfj.properties";
  public static final String KEY_PERFJ_LIB_PATH = "info.minzhou.perfj.lib.path";
  public static final String KEY_PERFJ_LIB_NAME = "info.minzhou.perfj.lib.name";
  public static final String KEY_PERFJ_TEMPDIR = "info.minzhou.perfj.tempdir";
  public static final String KEY_PERFJ_USE_SYSTEMLIB = "info.minzhou.perfj.use.systemlib";

  private static boolean hasResource(String path) {
    return PerfAgent.class.getResource(path) != null;
  }

  private static File extractLibraryFile(String libraryFolder, String libraryFileName, String targetFolder) {
    String nativeLibraryFilePath = libraryFolder + "/" + libraryFileName;

    // Attach UUID to the native library file to ensure multiple processes can read the libperfj multiple times.
    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("perfj-%s-%s", uuid, libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);

    try {
      // Extract a native library file into the target directory
      InputStream reader = null;
      FileOutputStream writer = null;
      try {
        reader = PerfAgent.class.getResourceAsStream(nativeLibraryFilePath);
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
          nativeIn = PerfAgent.class.getResourceAsStream(nativeLibraryFilePath);
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

  private static File findNativeLibrary() {
    boolean useSystemLib = Boolean.parseBoolean(System.getProperty(KEY_PERFJ_USE_SYSTEMLIB, "false"));
    if (useSystemLib) {
      return null; // Use a pre-installed libperfj
    }

    // Try to load the library in info.minzhou.perfj.lib.path  */
    String perfjNativeLibraryPath = System.getProperty(KEY_PERFJ_LIB_PATH);
    String perfjNativeLibraryName = System.getProperty(KEY_PERFJ_LIB_NAME);

    // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
    if (perfjNativeLibraryName == null) {
      perfjNativeLibraryName = System.mapLibraryName("perfj");
    }

    if (perfjNativeLibraryPath != null) {
      File nativeLib = new File(perfjNativeLibraryPath, perfjNativeLibraryName);
      if (nativeLib.exists()) {
        return nativeLib;
      }
    }

    // Load native library inside a jar file
    perfjNativeLibraryPath = "/info/minzhou/perfj/native";
    boolean hasNativeLib = hasResource(perfjNativeLibraryPath + "/" + perfjNativeLibraryName);

    if (!hasNativeLib) {
      throw new RuntimeException("no native library is found for perfj");
    }

    // Temporary folder for the native lib. Use the value of info.minzhou.perfj.tempdir or java.io.tmpdir
    File tempFolder = new File(System.getProperty(KEY_PERFJ_TEMPDIR, System.getProperty("java.io.tmpdir")));
    if (!tempFolder.exists()) {
      boolean created = tempFolder.mkdirs();
      if (!created) {
        // if created == false, it will fail eventually in the later part
      }
    }

    // Extract and load a native library inside the jar file
    return extractLibraryFile(perfjNativeLibraryPath, perfjNativeLibraryName, tempFolder.getAbsolutePath());
  }

  // for running after VM start
  public static void agentmain(final String args, final Instrumentation instrumentation) {
    // int passedInPID = Integer.valueOf(args[0])
  }

  public static void premain(final String args, final Instrumentation instrumentation) {
    try {
      String currentVMPID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
      // attach to self
      VirtualMachine vm = VirtualMachine.attach(currentVMPID);
      vm.loadAgentPath();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // TODO： upload the generated file to target directory in shared storage system
  }
}
