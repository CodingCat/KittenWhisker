package me.codingcat.happysparking;

import com.google.common.collect.Lists;
import org.apache.commons.lang.SystemUtils;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class Utils {

  private static List<NetworkInterface> getNetworkIFList(
          Enumeration<NetworkInterface> ifs, boolean reverse) {
    List<NetworkInterface> l = new ArrayList<>();
    while (ifs.hasMoreElements()) {
      l.add(ifs.nextElement());
    }
    if (reverse) {
      return Lists.reverse(l);
    } else {
      return l;
    }
  }

  static InetAddress localHostName() throws UnknownHostException, SocketException {
    String defaultIpOverride = System.getenv("SPARK_LOCAL_IP");
    if (defaultIpOverride != null) {
      return InetAddress.getByName(defaultIpOverride);
    } else {
      InetAddress address = InetAddress.getLocalHost();
      if (address.isLoopbackAddress()) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        Enumeration<NetworkInterface> activeNetworkIFs = NetworkInterface.getNetworkInterfaces();
        List<NetworkInterface> reOrderedNetworkIFs;

        if (SystemUtils.IS_OS_WINDOWS) {
          reOrderedNetworkIFs = getNetworkIFList(activeNetworkIFs, false);
        } else {
          reOrderedNetworkIFs = getNetworkIFList(activeNetworkIFs, true);
        }
        for (int i = 0; i < reOrderedNetworkIFs.size(); i++) {
          NetworkInterface ni = reOrderedNetworkIFs.get(i);
          Enumeration<InetAddress> addresses = ni.getInetAddresses();
          List<InetAddress> filteredAddresses = new ArrayList<>();
          while (addresses.hasMoreElements()) {
            InetAddress nextAddr = addresses.nextElement();
            if (!nextAddr.isLinkLocalAddress() && nextAddr.isLoopbackAddress()) {
              filteredAddresses.add(nextAddr);
            }
          }
          if (!filteredAddresses.isEmpty()) {
            InetAddress ipv4Addr = null;
            for (int j = 0; j < filteredAddresses.size(); j++) {
              if (filteredAddresses.get(j) instanceof Inet4Address) {
                ipv4Addr = filteredAddresses.get(j);
                break;
              }
            }
            return InetAddress.getByAddress(ipv4Addr.getAddress());
          }
        }
      }
      return address;
    }
  }
}
