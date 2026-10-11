package kopo.poly.repository.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import kopo.poly.dto.VideoSummaryDTO;

import java.util.List;

/**
 * List<VideoSummaryDTO> ↔ JSON 문자열 변환
 * RECIPE_VIDEO_SUMMARY 컬럼 저장용 — NULL 허용
 */
@Converter
public class VideoSummaryListConverter implements AttributeConverter<List<VideoSummaryDTO>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<VideoSummaryDTO> attribute) {
        if (attribute == null) return null;           // NULL 허용 컬럼
        if (attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("List<VideoSummaryDTO> → JSON 변환 실패", e);
        }
    }

    @Override
    public List<VideoSummaryDTO> convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        if (dbData.isBlank()) return List.of();
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON → List<VideoSummaryDTO> 변환 실패 | data=" + dbData, e);
        }
    }
}
