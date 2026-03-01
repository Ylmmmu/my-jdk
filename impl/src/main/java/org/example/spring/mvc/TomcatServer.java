package org.example.spring.mvc;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.example.spring.Autowired;
import org.example.spring.Component;
import org.example.spring.PostConstruct;

@Component
public class TomcatServer {

    @Autowired
    private DispatcherServlet dispatcherServlet;

    @PostConstruct
    void start() throws LifecycleException {
        int port = 8080;
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector();

        String contextPath = "";
        String docBase = new File(".").getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);

        tomcat.addServlet(contextPath, "dispatcherServlet", dispatcherServlet);
        context.addServletMappingDecoded("/*", "dispatcherServlet");
        tomcat.start();
        tomcat.getServer().await();
    }
}