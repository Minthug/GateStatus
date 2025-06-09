package com.example.GateStatus.domain.common;

import com.fasterxml.jackson.databind.JsonNode;


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
    public static  String getTextValue(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : "";
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

    /**
     * Json 필드 존재 여부 확인
     * @param node
     * @param fieldName
     * @return
     */
    public static boolean hasField(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull();
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }


}
