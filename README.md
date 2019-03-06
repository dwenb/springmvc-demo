# springmvc-demo

好像丢失了几次提交，不过最新提交的代码内容没有错，拉取代码不影响结果

手写源码，自定义实现Autowired、Controller、RequestMapping、
RequestParam、Service等注解。

servlet中主要用到
```$xslt
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
```    
这几个方法，其中要注意doInstance中的
newInstance()在servlet-api 3.0以后调用方式不同，
详见doInstance方法注释。
