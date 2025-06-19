package com.example.GateStatus.domain.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * JSON 데이터 처리를 위한 유틸리티 클래스
 */
public class JsonUtils {

    /**
     * JsonNode에서 텍스트 값 추출
     * @param node
     * @param fieldName
     * @return
     */
    public static String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText().trim() : "";
    }

    /**
     * JsonNode에서 정수 값 추출
     * @param node
     * @param fieldName
     * @return
     */
    public static int getIntValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return 0;
        }

        try {
            return field.asInt();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * JsonNode에서 불리언 값 추출
     * @param node
     * @param fieldName
     * @return
     */
    public static boolean getBooleanValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull() && field.asBoolean();
    }

    /**
     * JsonNode에서 실수 값 추출
     * @param node
     * @param fieldName
     * @return
     */
    public static double getDoubleValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field != null || field.isNull()) {
            return 0.0;
        }

        try {
            return field.asDouble();
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static long getLongValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) {
            return 0L;
        }

        try {
            return field.asLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    public static List<String> getStringListValue(JsonNode node, String fieldName) {
        String text = getTextValue(node, fieldName);
        if (isEmpty(text)) {
            return Collections.emptyList();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Json 필드 존재 여부 확인
     * @param node
     * @param fieldName
     * @return
     */
    public static boolean hasField(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull();
    }


    public static String getNestedTextValue(JsonNode node, String... paths) {
        JsonNode current = node;
        for (String path : paths) {
            if (current == null || !current.has(path)) {
                return "";
            }
            current = current.get(path);
        }
        return current != null && !current.isNull() ? current.asText().trim() : "";
    }

    /**
     * JsonNode 배열에서 특정 인덱스의 노드 추출
     * @param arrayNode 배열 노드
     * @param index 인덱스
     * @return JsonNode (인덱스가 범위를 벗어나면 null)
     */
    public static JsonNode getArrayElement(JsonNode arrayNode, int index) {
        if (arrayNode == null || !arrayNode.isArray() || index < 0 || index >= arrayNode.size()) {
            return null;
        }
        return arrayNode.get(index);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isEmpty(JsonNode node) {
        return node == null || node.isNull() ||
                (node.isTextual() && isEmpty(node.asText())) ||
                (node.isArray() && node.size() == 0) ||
                (node.isObject() && node.size() == 0);
    }
}