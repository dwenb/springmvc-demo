package com.spring.mvcframework.servlet;

import com.spring.mvcframework.annotation.DwbAutowired;
import com.spring.mvcframework.annotation.DwbController;
import com.spring.mvcframework.annotation.DwbRequestMapping;
import com.spring.mvcframework.annotation.DwbService;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    //跟web.xml中param-name的值一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有配置信息
    private Properties p = new Properties();

    //保存所有被扫描到的相关的类名
    private List<String> classNames = new ArrayList<String>();

    //核心IOC容器， 保存所有初始化的Bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public DispatcherServlet() {
        super();
    }

    /**
     *  初始化，加载配置文件
     *
     * @param config 配置
     */
    public void init(ServletConfig config) {
        //1、加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2、扫描所有相关的类
        doScanner(p.getProperty("scanPackage"));

        //3、初始化所有相关的实例，并保存到IOC容器中
        doInstance();

        //4、依赖注入
        doAutowired();

        //5、构造HandlerMapping
        initHandleMapping();

        //6、等待请求 匹配URL，定位方法，反射调用执行
        //调用doGet方法或doPost方法

        //提示信息
        System.out.println("spring mvc framework is init");

    }

    private void doLoadConfig(String location) {
        InputStream fis = null;
        try {
            fis = this.getClass().getClassLoader().getResourceAsStream(location);

            //读取配置文件
            if (null == fis){
                System.out.println("扫描文件不应该为空=============");
            }
            p.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doScanner(String packageName) {
        //将所有的包路径替换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是文件夹，继续递归
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replaceAll(".class", "").trim());
            }
        }
    }

    /**
     * IOC容器的key默认是类名首字母小写，如果是自己自己设置类名，则优先使用自定义的。
     *
     * @param str 类名
     * @return 仅转换首字母的字符串
     */
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     *
     *  下面的clazz.getDeclaredConstructor().newInstance()
     *  在3.0.1之前使用 clazz.newInstance()
     *
     */
    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String classNameItem : classNames) {
                Class<?> clazz = Class.forName(classNameItem);
                if (clazz.isAnnotationPresent(DwbController.class)) {
                    //默认首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.getDeclaredConstructor().newInstance());
                } else if (clazz.isAnnotationPresent(DwbService.class)) {
                    DwbService service = clazz.getAnnotation(DwbService.class);
                    String beanName = service.value();

                    //如果用户设置了名字，就用用户自己的设置
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.getDeclaredConstructor().newInstance());
                        continue;
                    }

                    //如果用户没设，就按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(), clazz.getDeclaredConstructor().newInstance());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void doAutowired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(DwbAutowired.class)) {
                    continue;
                }

                DwbAutowired autowired = field.getAnnotation(DwbAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }


    private void initHandleMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(DwbController.class)) {
                continue;
            }

            String baseUrl = "";

            //获取Controller的url配置
            if (clazz.isAnnotationPresent(DwbRequestMapping.class)) {
                DwbRequestMapping requestMapping = clazz.getAnnotation(DwbRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //获取Method的url值
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //没有加RequestMapping注解的直接忽略
                if (!method.isAnnotationPresent(DwbRequestMapping.class)) {
                    continue;
                }

                //映射URL
                DwbRequestMapping requestMapping = method.getAnnotation(DwbRequestMapping.class);
                String url = ("/" +baseUrl +"/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("mapped " + url + "," + method);
            }
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        this.doPost(req, res);
    }

    /**
     * 业务处理
     *
     * @param req
     * @param res
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            //开始匹配到对应的方法
            doDispatch(req, res);
        } catch (Exception e) {
            //如果匹配过程中出现异常，将异常值打印出去
            res.getWriter().write("500 Exception, Details: \r\n"
                    + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\}]", "")
                    .replaceAll(",\\s", "\r\n"));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (this.handlerMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            res.getWriter().write("404 Not Found");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //获取方法的参数列表
        Class<?>[] paramsTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String, String[]> paramterMap = req.getParameterMap();
        //保存参数值
        Object[] paramValues = new Object[paramsTypes.length];
        //方法的参数列表
        for (int i = 0; i < paramsTypes.length; i++) {
            //根据参数名称做某些处理，
            Class parameterType = paramsTypes[i];
            if (parameterType == HttpServletRequest.class) {
                //参数类型已明确，强制转类型
                paramValues[i] = req;
                continue;
            } else if (parameterType == HttpServletResponse.class) {
                paramValues[i] = res;
                continue;
            } else if (parameterType == String.class) {
                for (Map.Entry<String, String[]> param : paramterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll(",\\s", ",");
                    paramValues[i] = value;
                }
            }
        }

        try {
            String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
            //利用反射机制来调用
            method.invoke(this.ioc.get(beanName), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
