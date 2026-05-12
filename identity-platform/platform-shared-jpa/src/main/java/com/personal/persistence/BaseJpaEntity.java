package com.personal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Setter
@Getter
public class BaseJpaEntity {
    @Version
    @Column(name = "version")
    private Long version;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(name = "active", nullable = false, columnDefinition = "NUMBER(1,0) DEFAULT 1")
    private Boolean active = true;

    @Convert(converter = BooleanToIntegerConverter.class)
    @Column(name = "deleted", nullable = false, columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private Boolean deleted = false;

}
