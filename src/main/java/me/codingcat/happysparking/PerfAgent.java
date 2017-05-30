package me.codingcat.happysparking;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.UUID;

import com.sun.tools.attach.VirtualMachine;

public class PerfAgent {

  private static boolean hasResource(String path) {
    return PerfAgent.class.getResource(path) != null;
  }

  private static File extractLibraryFile(String libraryFileName, String targetFolder) {

    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("happysparking-%s-%s", uuid, libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);

    try {
      // Extract a native library file into the target directory
      InputStream reader = null;
      FileOutputStream writer = null;
      try {
        reader = PerfAgent.class.getResourceAsStream(libraryFileName);
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
          nativeIn = PerfAgent.class.getResourceAsStream(libraryFileName);
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
    // Load native library inside a jar file
    String perfjNativeLibraryPath = "/libperfmap.so";
    boolean hasNativeLib = hasResource(perfjNativeLibraryPath);

    if (!hasNativeLib) {
      throw new RuntimeException("no native library is found for happysparking");
    }

    // Temporary folder for the native lib. Use the value of info.minzhou.perfj.tempdir or java.io.tmpdir
    File tempFolder = new File(System.getProperty(System.getProperty("java.io.tmpdir")));
    if (!tempFolder.exists()) {
      boolean created = tempFolder.mkdirs();
      if (!created) {
        // if created == false, it will fail eventually in the later part
      }
    }
    // Extract and load a native library inside the jar file
    return extractLibraryFile(perfjNativeLibraryPath, tempFolder.getAbsolutePath());
  }

  // TODO: for running after VM start
  public static void agentmain(final String args, final Instrumentation instrumentation) {
    // int passedInPID = Integer.valueOf(args[0])
  }

  public static void premain(final String args, final Instrumentation instrumentation) {
    try {
      File f = findNativeLibrary();
      String options = args;
      String currentVMPID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
      // attach to self
      VirtualMachine vm = VirtualMachine.attach(currentVMPID);
      vm.loadAgentPath(f.getAbsolutePath(), options);
    } catch (Exception e) {
      e.printStackTrace();
    }
    // TODO： upload the generated file to target directory in shared storage system
  }
}
