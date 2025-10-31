package com.lfs.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class JsonToJavaService {

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}(\\s|T)\\d{2}:\\d{2}:\\d{2}$|\\d{4}-\\d{2}-\\d{2}");

    public String convert(String jsonString) {
        if (StrUtil.isBlank(jsonString)) {
            return "";
        }
        try {
            JSONObject jsonObject = JSONUtil.parseObj(jsonString);
            StringBuilder fields = new StringBuilder();
            List<String> innerClasses = new ArrayList<>();

            generateFields(jsonObject, fields, innerClasses);

            StringBuilder result = new StringBuilder(fields);
            for (String innerClass : innerClasses) {
                result.append("\n").append(innerClass);
            }

            return result.toString();
        } catch (JSONException e) {
            return "Error: Invalid JSON format.";
        }
    }

    private void generateFields(JSONObject jsonObject, StringBuilder fields, List<String> innerClasses) {
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            Pair<String, String> typeAndAnnotation = getJavaTypeAndAnnotation(value, fieldName, innerClasses);
            String javaType = typeAndAnnotation.getKey();
            String annotation = typeAndAnnotation.getValue();
            if (annotation != null) {
                fields.append("    ").append(annotation).append("\n");
            }
            fields.append("    private ").append(javaType).append(" ").append(StrUtil.toCamelCase(fieldName)).append(";\n");
        }
    }

    private Pair<String, String> getJavaTypeAndAnnotation(Object value, String fieldName, List<String> innerClasses) {
        if (value instanceof String) {
            if (isDate((String) value)) {
                String format = detectDateFormat((String) value);
                return new Pair<>("LocalDateTime", "@com.fasterxml.jackson.annotation.JsonFormat(pattern = \"" + format + "\")");
            }
            return new Pair<>("String", null);
        } else if (value instanceof Integer || value instanceof Long) {
            return new Pair<>("Long", null);
        } else if (value instanceof Double || value instanceof Float) {
            return new Pair<>("Double", null);
        } else if (value instanceof Boolean) {
            return new Pair<>("Boolean", null);
        } else if (value instanceof JSONArray) {
            return new Pair<>(handleJsonArray((JSONArray) value, fieldName, innerClasses), null);
        } else if (value instanceof JSONObject) {
            return new Pair<>(handleJsonObject((JSONObject) value, fieldName, innerClasses), null);
        }
        return new Pair<>("Object", null);
    }

    private String handleJsonObject(JSONObject jsonObject, String fieldName, List<String> innerClasses) {
        String innerClassName = StrUtil.upperFirst(StrUtil.toCamelCase(fieldName));
        StringBuilder innerClassFields = new StringBuilder();
        generateFields(jsonObject, innerClassFields, innerClasses);

        StringBuilder innerClass = new StringBuilder();
        innerClass.append("\n@lombok.Data\n");
        innerClass.append("public static class ").append(innerClassName).append(" {\n");
        innerClass.append(innerClassFields);
        innerClass.append("}");

        innerClasses.add(innerClass.toString());
        return innerClassName;
    }

    private String handleJsonArray(JSONArray jsonArray, String fieldName, List<String> innerClasses) {
        if (jsonArray.isEmpty()) {
            return "List<Object>";
        }
        Object firstElement = jsonArray.get(0);
        String singularFieldName = fieldName.endsWith("s") ? fieldName.substring(0, fieldName.length() - 1) : fieldName;
        Pair<String, String> typeAndAnnotation = getJavaTypeAndAnnotation(firstElement, singularFieldName, innerClasses);
        return "List<" + typeAndAnnotation.getKey() + ">";
    }

    private boolean isDate(String value) {
        if (value == null) {
            return false;
        }
        return DATE_PATTERN.matcher(value).matches();
    }

    private String detectDateFormat(String value) {
        if (value.contains("T")) {
            return "yyyy-MM-dd'T'HH:mm:ss";
        } else if (value.contains(" ")) {
            return "yyyy-MM-dd HH:mm:ss";
        } else {
            return "yyyy-MM-dd";
        }
    }

    private static class Pair<K, V> {
        private final K key;
        private final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
