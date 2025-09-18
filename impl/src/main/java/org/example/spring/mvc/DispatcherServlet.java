package org.example.spring.mvc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.example.spring.Component;
import org.example.spring.FactoryImpl;
import org.example.spring.PostProcessor;

@Component
public class DispatcherServlet extends HttpServlet implements PostProcessor {

    Map<String, MethodHandler> route = new HashMap<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        System.out.println(path);
        MethodHandler methodHandler = route.get(path);
        if (methodHandler == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (methodHandler != null) {
            try {
                // 调用控制器方法并返回结果
                String view = methodHandler.handleRequest(req, resp);

            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "404 Not Found");
        }
    }


    @Override
    public Object postProcessAfterInitialization(Object bean) {
        Class<?> clazz = bean.getClass();
        if (clazz.isAnnotationPresent(RequestMapping.class)) {
            String mainPath = clazz.getAnnotation(RequestMapping.class).path();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    String path = mainPath + method.getAnnotation(RequestMapping.class).path();
                    route.put(path, new MethodHandler(bean, method, path));
                }
            }
        }
        return bean;
    }
}
