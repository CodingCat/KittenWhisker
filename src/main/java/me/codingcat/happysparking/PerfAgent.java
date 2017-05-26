package me.codingcat.happysparking;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;

public class PerfAgent {

  // for running after VM start
  public static void agentmain(final String args, final Instrumentation instrumentation) {

  }

  public static void premain(final String args, final Instrumentation instrumentation) {
    // fork a new process and monitor the current one
    try {
      int currentVMPID = Integer.valueOf(
              ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
      ProcessBuilder pb = new ProcessBuilder("xxx");
      Process p = pb.start();
      p.waitFor();
    } catch (Exception e) {
      e.printStackTrace();
    }
    // TODO： upload the generated file to target directory
  }
}
