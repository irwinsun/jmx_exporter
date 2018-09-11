package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

public class JavaAgent {

   static HTTPServer server;

   public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
     premain(agentArgument, instrumentation);
   }

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     // Bind to all interfaces by default (this includes IPv6).
     String host = "0.0.0.0";

     // If we have IPv6 address in square brackets, extract it first and then
     // remove it from arguments to prevent confusion from too many colons.
     int indexOfClosingSquareBracket = agentArgument.indexOf("]:");
     if (indexOfClosingSquareBracket >= 0) {
       host = agentArgument.substring(0, indexOfClosingSquareBracket + 1);
       agentArgument = agentArgument.substring(indexOfClosingSquareBracket + 2);
     }

     String[] args = agentArgument.split(":");
     if (args.length < 2 || args.length > 3) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[daemon@][host:]<port>:<yaml configuration file>");
       System.exit(1);
     }

     int port;
     String file;
     InetSocketAddress socket;
     boolean daemon = true;
     if (args[0].contains("@")) {
       String[] daemonHost = args[0].split("@");
       daemon = Boolean.valueOf(daemonHost[0]);
       args[0] = daemonHost[1];
     }
     if (args.length == 3) {
       port = Integer.parseInt(args[1]);
       socket = new InetSocketAddress(args[0], port);
       file = args[2];
     } else {
       port = Integer.parseInt(args[0]);
       socket = new InetSocketAddress(host, port);
       file = args[1];
     }

     new BuildInfoCollector().register();
     new JmxCollector(new File(file)).register();
     DefaultExports.initialize();
     server = new HTTPServer(socket, CollectorRegistry.defaultRegistry, daemon);
   }
}
