package com.ksyun.train.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

public class ParamParseUtil {

    public static <T> T parse(Class<T> clz, String queryString) throws Exception {
        T t = clz.newInstance();
        if (queryString == null || queryString.isEmpty()) {
            return t;
        }
        Map<String, List<String>> paramMap = parseQueryString(queryString);
        setFields(t, paramMap);
        return t;
    }

    /**
     * 解析queryString，返回一个参数map，键为参数名，值为参数值列表
     */
    private static Map<String, List<String>> parseQueryString(String queryString) {
        Map<String, List<String>> paramMap = new HashMap<>();
        String[] paramPairs = queryString.split("&");
        for (String pair : paramPairs) {
            int pos = pair.indexOf('=');
            if (pos > 0) {
                String name = pair.substring(0, pos);
                String value = pair.substring(pos + 1);
                if (!name.isEmpty()) {
                    if (!paramMap.containsKey(name)) {
                        paramMap.put(name, new ArrayList<>());
                    }
                    paramMap.get(name).add(value);
                }
            }
        }
        return paramMap;
    }

    /**
     * 使用反射设置对象的属性值
     */
    private static void setFields(Object obj, Map<String, List<String>> paramMap) throws Exception {
        Class<?> clz = obj.getClass();
        for (Field field : clz.getDeclaredFields()) {
            if (field.isAnnotationPresent(SkipMappingValueAnnotation.class)) {
                // 跳过标注了SkipMappingValueAnnotation注解的属性
                continue;
            }
            String fieldName = field.getName().toLowerCase();
            String upperFieldName = capitalizeFirstLetter(fieldName);
            List<String> valueList = paramMap.get(upperFieldName);
            if (valueList == null || valueList.isEmpty()) {
                // 参数中没有该属性的值，跳过
                continue;
            }
            Object fieldValue = null;
            Class<?> fieldType = field.getType();
            if (fieldType.isArray()) {
                // 处理数组类型
                Class<?> componentType = fieldType.getComponentType();
                fieldValue = Array.newInstance(componentType, valueList.size());
                for (int i = 0; i < valueList.size(); i++) {
                    Array.set(fieldValue, i, parseValue(componentType, valueList.get(i)));
                }
            } else if (List.class.isAssignableFrom(fieldType)) {
                // 处理List类型
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length > 0) {
                        Class<?> elementType = (Class<?>) actualTypeArguments[0];
                        List<Object> list = new ArrayList<>();
                        for (String value : valueList) {
                            list.add(parseValue(elementType, value));
                        }
                        // 按照数字大小对List进行排序
                        Collections.sort(list, (a, b) -> {
                            String aName = getContainerName(a);
                            String bName = getContainerName(b);
                            return Integer.compare(getNumberFromContainerName(aName), getNumberFromContainerName(bName));
                        });
                        fieldValue = list;
                    }
                }
            } else if (fieldType.isAssignableFrom(Map.class)) {
                // 处理Map类型
                Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments.length >= 2) {
                        Class<?> keyType = (Class<?>) actualTypeArguments[0];
                        Class<?> valueType = (Class<?>) actualTypeArguments[1];
                        Map<Object, Object> map = new HashMap<>();
                        for (String value : valueList) {
                            String[] parts = value.split(":");
                            if (parts.length == 2) {
                                Object key = parseValue(keyType, parts[0]);
                                Object val = parseValue(valueType, parts[1]);
                                map.put(key, val);
                            }
                        }
                        fieldValue = map;
                    }
                }
            } else if (fieldType.isPrimitive() || fieldType == String.class || fieldType == Integer.class || fieldType == Long.class || fieldType== Double.class || fieldType == Float.class || fieldType == Short.class || fieldType == Byte.class || fieldType == Character.class || fieldType == Boolean.class || fieldType == BigDecimal.class) {
                // 处理基本类型和String、BigDecimal类型
                fieldValue = parseValue(fieldType, valueList.get(0));
            } else {
                // 处理其他类型，递归调用setFields方法设置嵌套对象的属性值
                Object nestedObj = fieldType.newInstance();
                setFields(nestedObj, paramMap);
                fieldValue = nestedObj;
            }
            field.setAccessible(true);
            field.set(obj, fieldValue);
        }
    }

    /**
     * 解析字符串类型的属性值，返回对应类型的值
     */
    private static <T> T parseValue(Class<T> type, String valueStr) {
        if (type == String.class) {
            return type.cast(valueStr);
        } else if (type == Integer.class || type == int.class) {
            return type.cast(Integer.parseInt(valueStr));
        } else if (type == Long.class || type == long.class) {
            return type.cast(Long.parseLong(valueStr));
        } else if (type == Double.class || type == double.class) {
            return type.cast(Double.parseDouble(valueStr));
        } else if (type == Float.class || type == float.class) {
            return type.cast(Float.parseFloat(valueStr));
        } else if (type == Short.class || type == short.class) {
            return type.cast(Short.parseShort(valueStr));
        } else if (type == Byte.class || type == byte.class) {
            return type.cast(Byte.parseByte(valueStr));
        } else if (type == Character.class || type == char.class) {
            return type.cast(valueStr.charAt(0));
        } else if (type == Boolean.class || type == boolean.class) {
            return type.cast(Boolean.parseBoolean(valueStr));
        } else if (type == BigDecimal.class) {
            return type.cast(new BigDecimal(valueStr));
        }
        return null;
    }

    /**
     * 将字符串的首字母大写
     */
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 获取容器对象的名称，用于按照数字大小排序List
     */
    private static String getContainerName(Object obj) {
        if (obj == null) {
            return "";
        }
        Class<?> clz = obj.getClass();
        if (clz.isArray()) {
            return clz.getComponentType().getSimpleName();
        } else if (List.class.isAssignableFrom(clz)) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) {
                return "";
            }
            return list.get(0).getClass().getSimpleName();
        } else {
            return clz.getSimpleName();
        }
    }

    /**
     * 从容器对象的名称中获取数字，用于按照数字大小排序List
     */
    private static int getNumberFromContainerName(String name) {
        if (name == null || name.isEmpty()) {
            return -1;
        }
        int i = name.length() - 1;
        while (i >= 0 && Character.isDigit(name.charAt(i))) {
            i--;
        }
        if (i < 0) {
            return -1;
        }
        return Integer.parseInt(name.substring(i + 1));
    }

}