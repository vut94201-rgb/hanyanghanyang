package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.shared.enums.CodeEnum;
import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractCodeEnumConverter<E extends Enum<E> & CodeEnum<String>>
    implements AttributeConverter<E, String> {
  private final Map<String, E> codeMap;

  protected AbstractCodeEnumConverter(Class<E> enumClass) {
    this.codeMap =
        Arrays.stream(enumClass.getEnumConstants())
            .collect(Collectors.toUnmodifiableMap(CodeEnum::getCode, Function.identity()));
  }

  @Override
  public String convertToDatabaseColumn(E attribute) {
    return Objects.isNull(attribute) ? null : attribute.getCode();
  }

  @Override
  public E convertToEntityAttribute(String dbData) {

    if (Objects.isNull(dbData) || dbData.isBlank()) {
      return null;
    }
    E value = codeMap.get(dbData);
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException("Unknown enum code: " + dbData);
    }
    return value;
  }
}
