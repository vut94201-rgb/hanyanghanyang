package com.personal.identityservicealmav1.authentication.domain.entities;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq", allocationSize = 1)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_password", columnList = "password"),
                @Index(name = "idx_user_phone", columnList = "phone"),
                @Index(name = "idx_user_full_name", columnList = "full_name"),
                @Index(name = "idx_user_deleted", columnList = "deleted"),
                @Index(name = "idx_user_active_deleted", columnList = "active,deleted"),
                @Index(name = "idx_user_active_deleted_email", columnList = "email,active,deleted"),
                @Index(name = "idx_user_active_deleted_username", columnList = "username,active,deleted"),
                @Index(name = "idx_user_active_deleted_name", columnList = "full_name,active,deleted"),
                @Index(name = "idx_user_active_deleted_phone", columnList = "phone,active,deleted"),
                @Index(name = "idx_user_id_active_deleted", columnList = "id,active,deleted")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_user_username", columnNames = "username"),
                @UniqueConstraint(name = "unique_user_phone", columnNames = "phone"),
                @UniqueConstraint(name = "unique_user_email", columnNames = "email")
        })
public class User extends BaseModel {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
    private Long id;

    @Column(name = "username", unique = true)
    String username;

    String password;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "address", unique = false)
    String address;

    @Column(name = "phone", unique = true)
    String phone;

    @Column(name = "avatar_path")
    String avatarPath;

    @Column(name = "full_name")
    String fullName;

    @Enumerated(value = EnumType.STRING)
    Gender gender;
    @Column(name = "locked", columnDefinition = "NUMBER(1,0) DEFAULT 1")
    Boolean userLocked ;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;
    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    LocalDate dateOfBirth;
    Integer maxSession;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @ToString.Exclude
    Set<Role> roles;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_permission",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @ToString.Exclude
    Set<Permission> permissions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    Set<Token> tokens;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        this.userLocked = false;
        this.failedAttempts = 0;
        this.lockTime = null;
    }
}

