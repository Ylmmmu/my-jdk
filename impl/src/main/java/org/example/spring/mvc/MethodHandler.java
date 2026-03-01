package org.example.spring.mvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

@Data
public class MethodHandler {

    private Object target;

    private Method method;

    private String path;

    public MethodHandler(Object name, Method method, String path) {
        this.target = name;
        this.method = method;
        this.path = path;
    }


    public String handleRequest(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        try {
            Parameter[] parameters = method.getParameters();
            List<Object> params = new ArrayList<>();
            Object[] array = Arrays.stream(parameters).map(
                    parameter -> {
                        if (parameter.isAnnotationPresent(RequestBody.class)) {
                            try {
                                BufferedReader reader = req.getReader();
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    sb.append(line);
                                }
                                ObjectMapper mapper = new ObjectMapper();
                                return mapper.readValue(sb.toString(), parameter.getType());
                            } catch (Exception e) {
                                return null;
                            }
                        } else {
                            String name = parameter.getName();
                            if (parameter.isAnnotationPresent(RequestParam.class)) {
                                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                                if (StringUtils.isNoneBlank(requestParam.value())) {
                                    name = requestParam.value();
                                }
                                if (requestParam.required() && req.getParameter(name) == null){
                                    throw new RuntimeException(name + " 参数不能为空");
                                }
                            }
                            return req.getParameter(name);
                        }
                    }
            ).collect(Collectors.toList()).toArray();
            Object invoke = method.invoke(target, array);
            ObjectMapper mapper = new ObjectMapper();
            String s = mapper.writeValueAsString(invoke);
            PrintWriter writer = resp.getWriter();
            writer.write(s);
            return s;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
