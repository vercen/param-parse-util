package com.ksyun.train.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParamParseUtil {

    public static <T> T parse(Class<T> clz, String queryString) throws Exception {
        // 将queryString转换为键值对的形式
        Map<String, String> paramMap = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                paramMap.put(keyValue[0], keyValue[1]);
            }
        }

        // 创建clz对应的实例
        T instance = clz.newInstance();

        // 遍历paramMap，设置属性值
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 忽略小写开头的参数
            if (Character.isLowerCase(key.charAt(0))) {
                continue;
            }

            // 查找属性对应的Field对象
            Field field = findField(clz, key.toLowerCase());

            // 如果没有找到对应的属性，忽略该参数
            if (field == null) {
                continue;
            }

            // 如果属性上标注了@SkipMappingValueAnnotation注解，则不设置该属性的值，保留默认值
            if (field.getAnnotation(SkipMappingValueAnnotation.class) != null) {
                continue;
            }

            // 根据Field对象的类型进行类型转换
            Class<?> fieldType = field.getType();
            Object fieldValue = null;
            if (fieldType == String.class) {
                fieldValue = value;
            } else if (fieldType == Integer.class || fieldType == int.class) {
                fieldValue = Integer.valueOf(value);
            } else if (fieldType == Long.class || fieldType == long.class) {
                fieldValue = Long.valueOf(value);
            } else if (fieldType == Double.class || fieldType == double.class) {
                fieldValue = Double.valueOf(value);
            } else if (fieldType == Float.class || fieldType == float.class) {
                fieldValue = Float.valueOf(value);
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                fieldValue = Boolean.valueOf(value);
            } else if (fieldType == BigDecimal.class) {
                fieldValue = new BigDecimal(value);
            } else if (List.class.isAssignableFrom(fieldType)) { // 判断是否为List类型
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length == 1 && typeArguments[0] instanceof Class) {
                        Class<?> listType = (Class<?>) typeArguments[0];
                        List<Object> listValue = getListValue(listType, paramMap, key.toLowerCase());
                        fieldValue = listValue;
                    }
                }
            } else { // 自定义对象类型
                Object subInstance = parse(fieldType, queryString);
                fieldValue = subInstance;
            }

            // 设置属性值
            field.setAccessible(true);
            field.set(instance, fieldValue);
        }

        return instance;
    }

    private static Field findField(Class<?> clz, String fieldName) {
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(fieldName)) {
                return field;
            }
        }
        Class<?> superClass = clz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findField(superClass, fieldName);
        }
        return null;
    }

    private static List<Object> getListValue(Class<?> elementType, Map<String, String> paramMap, String prefix) {
        List<Object> listValue = new ArrayList<>();
        int index = 1;
        while (true) {
            String key = prefix + "." + index;
            if (!paramMap.containsKey(key)) {
                break;
            }
            String value = paramMap.get(key);
            try {
                Object elementValue = null;
                if (elementType == String.class) {
                    elementValue = value;
                } else if (elementType == Integer.class || elementType == int.class) {
                    elementValue = Integer.valueOf(value);
                } else if (elementType == Long.class || elementType == long.class) {
                    elementValue = Long.valueOf(value);
                } else if (elementType == Double.class || elementType == double.class) {
                    elementValue = Double.valueOf(value);
                } else if (elementType == Float.class || elementType == float.class) {
                    elementValue = Float.valueOf(value);
                } else if (elementType == Boolean.class || elementType == boolean.class) {
                    elementValue = Boolean.valueOf(value);
                } else if (elementType == BigDecimal.class) {
                    elementValue = new BigDecimal(value);
                } else {
                    // 解析自定义对象类型
                    Map<String, String> subParamMap = new HashMap<>();
                    for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                        String subKey = entry.getKey();
                        if (subKey.startsWith(key + ".")) {
                            subParamMap.put(subKey.substring(key.length() + 1), entry.getValue());
                        }
                    }
                    elementValue = parse(elementType, subParamMap, "");
                }
                listValue.add(elementValue);
            } catch (Exception e) {
                // 忽略解析失败的元素
            }
            index++;
        }
        return listValue;
    }

    private static <T> T parse(Class<T> clz, Map<String, String> paramMap, String prefix) throws Exception {
        // 构造子对象的queryString
        StringBuilder queryStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix + ".")) {
                queryStringBuilder.append(key.substring(prefix.length() + 1))
                        .append("=")
                        .append(entry.getValue())
                        .append("&");
            }
        }
        String queryString = queryStringBuilder.toString();
        if (queryString.endsWith("&")) {
            queryString = queryString.substring(0, queryString.length() - 1);
        }

        // 解析子对象
        return parse(clz, queryString);
    }
}