package com.example.logistics.entity;

import com.example.logistics.enums.LogisticsEventType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LogisticsEventTypeConverter implements AttributeConverter<LogisticsEventType, String> {

    @Override
    public String convertToDatabaseColumn(LogisticsEventType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public LogisticsEventType convertToEntityAttribute(String dbData) {
        return LogisticsEventType.fromLegacyValue(dbData);
    }
}
