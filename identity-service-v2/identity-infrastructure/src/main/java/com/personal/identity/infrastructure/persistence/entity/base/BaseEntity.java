package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Version;

import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity<ID extends Serializable> implements Serializable {

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  public abstract ID getId();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (this.getClass() != o.getClass()) return false;
    BaseEntity<?> other = (BaseEntity<?>) o;
    return getId() != null && Objects.equals(getId(), other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
