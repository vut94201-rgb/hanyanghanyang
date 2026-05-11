package com.personal.identityservicealmav1.shared_config.shared.base.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseModel>
        extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    List<T> findByActiveAndDeletedFalse(boolean active);

    List<T> findByActiveAndDeletedFalse(boolean active, Pageable pageable);

    Page<T> findPageByAndActiveAndDeletedFalse(boolean active, Pageable pageable);

    List<T> findByActiveAndDeletedFalse(boolean active, Sort sort);

    T findByAndIdAndDeletedFalse(Long id);

    List<T> findByDeletedFalse(Pageable pageable);

    Page<T> findAllByDeletedFalse(Pageable pageable);

    List<T> findByDeletedFalse(Sort sort);

    @Query("SELECT t FROM #{#entityName} t WHERE  (:active is null OR t.active=:active)")
    List<T> findByActiveAndDeletedFalse(@Param("active") Boolean active, Sort sort);

    Page<T> findByActive(boolean active, Pageable pageable);

    String FIND_ALL_QUERY =
            "SELECT t FROM #{#entityName} t WHERE (:active IS NULL OR t.active = :active) AND (t.deleted = false OR t.deleted IS NULL)";

    @Query(FIND_ALL_QUERY)
    List<T> findAll(Boolean active);

    @Query(FIND_ALL_QUERY)
    Page<T> findAll(Boolean active, Pageable pageable);

    @Query("SELECT t FROM #{#entityName} t WHERE  (:active is null OR t.active=:active) AND t.id=:id")
    T findByActiveAndIdAndDeletedFalse(@Param("active") Boolean active, @Param("id") Long id);

    @Transactional
    @Modifying
    default void deleteBySpecification(Specification<T> specification) {
        final List<T> entities = findAll(specification);
        deleteAll(entities);
    }

    @Transactional
    @Modifying
    default void softDeleteBySpecification(Specification<T> specification) {
        final List<T> entities = findAll(specification);
        for (T entity : entities) {
            entity.setDeleted(true);
            save(entity);
        }
    }

    @Transactional
    @Modifying
    @Query("UPDATE #{#entityName} t SET t.deleted = true WHERE t.id = :id ")
    void softDeleteById(@Param("id") Long id);
}
