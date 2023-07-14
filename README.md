# 项目名称

这是一个Java工具类库，提供了将QueryString字符串转化为Java对象的方法。

## 安装和使用

1. 将ParamParseUtil.java文件拷贝到您的项目中。
2. 在您的Java代码中引入ParamParseUtil类。
3. 调用ParamParseUtil.parse方法，将QueryString字符串和Java类的Class对象作为输入参数，获取转化后的Java对象。

示例代码：

```java
String queryString = "person.name=张三&person.age=18&person.address.city=北京&person.address.street=朝阳路&scores=80&scores=90&scores=70";
Person person = ParamParseUtil.parse(Person.class, queryString);
```

## API文档

### ParamParseUtil类

#### parse方法

```java
public static <T> T parse(Class<T> clz, String queryString) throws Exception
```

将QueryString字符串转化为Java对象。

输入参数：

- clz：Java类的Class对象。
- queryString：QueryString字符串。

输出结果：

- 返回转化后的Java对象。
