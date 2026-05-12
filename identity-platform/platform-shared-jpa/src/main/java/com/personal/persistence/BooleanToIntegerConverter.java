package com.personal.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
/**
 * JPA {@link AttributeConverter} mapping {@link Boolean} ↔ {@link Integer}.
 *
 * <p>Oracle does not have a native boolean column type; the convention is
 * {@code NUMBER(1,0)} with values {@code 1} (true) and {@code 0} (false).
 * This converter handles the translation transparently so domain code
 * works with plain {@code Boolean}.
 *
 * <p>{@code autoApply = false}: this converter is opt-in per field via
 * {@code @Convert(converter = BooleanToIntegerConverter.class)}.
 * That avoids surprising behavior if a future entity legitimately needs
 * a different boolean encoding (e.g. {@code 'Y'/'N'} char column from a
 * legacy table).
 *
 * <p>Null handling: {@code null} stays {@code null} in both directions.
 * That preserves SQL NULL semantics — useful for columns where "unknown"
 * is meaningful. If the column is {@code NOT NULL} (as in
 * {@code BaseJpaEntity#active/deleted}), the entity field default
 * ({@code true} / {@code false}) prevents nulls from reaching the DB.
 */
@Converter(autoApply = false)
public class BooleanToIntegerConverter implements AttributeConverter<Boolean,Integer> {
    @Override
    public Integer convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute ? 1:0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer dbData) {

       if (dbData==null)return null;
       return dbData !=0;
    }
}
