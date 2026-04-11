package kopo.poly.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "USER_INFO")
@DynamicInsert
@DynamicUpdate
@Builder
@Cacheable
@Entity
public class UserInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID", updatable = false)
    private Integer userId;

    @NonNull
    @Column(name = "USER_NAME", nullable = false, length = 24)
    private String userName;

    @NonNull
    @Column(name = "PHONE_NUM", nullable = false, length = 20)
    private String phoneNum;

    @NonNull
    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    private String email;

    @NonNull
    @Column(name = "PASSWORD", nullable = false, length = 64)
    private String password;

    @Column(name = "REG_DT", updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호는 빈 값일 수 없습니다.");
        }
        this.password = newPassword;
    }
}
