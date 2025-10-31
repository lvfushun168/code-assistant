package com.lfs.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaToJsonService {
    private static final HashMap<String, Object> typeMap;
    private static final String blank = "    ";

    public String transformJavaBean(String javaBean) {
        if (javaBean == null || javaBean.trim().isEmpty()) {
            return "{}";
        }
        String str = javaBean.trim()
                .replaceAll("private|public|protected|static|final|transient|volatile", " ")   //去掉private|public|protected|static|final|transient|volatile
                .replaceAll("/\\*{1,2}[\\s\\S]*?\\*/", "")      //去掉/*注释*/
                .replaceAll("/\\*\\*(.*?)\\*/", "")   //去掉/**注释*/
                .replaceAll("//.*", "").replaceAll("//","")    //去掉//注释
                .replaceAll("(@((.|\\n)+?)\\n((.|\\n)+?))","").replaceAll("@((.|\\n)+?)+","")       //去掉@注解
                .replaceAll("\n\\s+","");
        str = str.trim();
        String[] typeAndFieldArr = str.split(";");
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (String typeAndField : typeAndFieldArr) {
            typeAndField = typeAndField.trim();
            if (typeAndField.isEmpty()) {
                continue;
            }
            String[] split = typeAndField.split("\\s+");
            if (split.length < 2) {
                continue;
            }
            String type = split[0];
            String field = split[1];
            Object typeValue = typeMap.get(type);
            if (typeValue == null) {
                Matcher matcher = Pattern.compile("<.*>").matcher(type);
                if (matcher.find()) {
                    typeValue = this.makeGenericTypeFieldStr(type);
                } else {
                    typeValue = "\"" + field + "\"";
                }
            }
            sb.append("\n").append(blank).append("\"").append(field).append("\"").append(": ").append(typeValue).append(",");
        }
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("\n}");
        return sb.toString();
    }

    private Object makeGenericTypeFieldStr(String field) {
        Object typeValue = typeMap.get(field);
        if (typeValue == null) {
            if (field.startsWith("List") || field.startsWith("ArrayList")) {
                String inner = field.substring(field.indexOf("<") + 1, field.lastIndexOf(">"));
                String outer = field.replace("<" + inner + ">", "");
                return typeMap.get(outer).toString().replace("\"element\"", makeGenericTypeFieldStr(inner).toString());
            } else if (field.startsWith("Map") || field.startsWith("HashMap")) {
                String inner = field.substring(field.indexOf("<") + 1, field.lastIndexOf(">"));
                String outer = field.replace("<" + inner + ">", "");
                String[] split = inner.split(",");
                return typeMap.get(outer).toString()
                        .replace("\"key\"", makeGenericTypeFieldStr(split[0]).toString())
                        .replace("\"value\"", makeGenericTypeFieldStr(split[1].trim()).toString());
            } else {
                return "\"" + field + "\"";
            }
        }
        return typeValue;
    }

    static {
        typeMap = new HashMap<>() {
            {
                put("String", "\"String内容\"");
                put("Integer", 10);
                put("Long", 1000L);
                put("Double", 2.0D);
                put("Float", 3.0F);
                put("Boolean", true);
                put("Character", 'a');
                put("Byte", (byte) 1);
                put("Short", (short) 1);
                put("Date", "\"2020-01-01\"");
                put("LocalDate", "\"" + LocalDate.now() + "\"");
                put("LocalDateTime", "\"2020-01-01 22:13:31\"");
                put("BigDecimal", new BigDecimal(1));
                put("List", "[\"element\"]");
                put("ArrayList", "[\"element\"]");
                put("Map", "{\"key\":\"value\"}");
                put("HashMap", "{\"key\":\"value\"}");
                put("Set", "[\"element\"]");
                put("HashSet", "[\"element\"]");
            }
        };
    }
}
