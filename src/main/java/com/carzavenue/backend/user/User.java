package com.carzavenue.backend.user;

import jakarta.persistence.*;
import jakarta.persistence.Convert;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    private String phoneNumber;

    @Builder.Default
    @Convert(converter = RoleAttributeConverter.class)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "google_sub", unique = true)
    private String googleSub;

    @Builder.Default
    @Column(nullable = false)
    private boolean isBlocked = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
