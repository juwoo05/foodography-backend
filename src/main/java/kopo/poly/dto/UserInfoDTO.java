package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kopo.poly.repository.entity.UserInfoEntity;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record UserInfoDTO(

        // Integer 타입 → @NotBlank / @Size 적용 불가 → @NotNull 사용
        @NotNull(message = "회원 아이디는 필수입니다.")
        Integer userId,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 24, message = "이름은 2자 이상 24자 이하로 입력해주세요.")
        String userName,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Size(min = 10, max = 20, message = "전화번호는 10자 이상 20자 이하로 입력해주세요.")
        String phoneNum,

        @NotBlank(message = "이메일은 필수입니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하로 입력해주세요.")
        String password,

        LocalDateTime regDt,

        String existYn,

        String roles,

        int authNumber

) {

    /**
     * 회원가입 시 암호화된 비밀번호와 권한을 적용한 DTO 생성
     *
     * @param pDTO     클라이언트 입력 DTO (userName, phoneNum, email 포함)
     * @param password BCrypt 암호화된 비밀번호
     * @param roles    부여할 권한 (예: "ROLE_USER")
     */
    public static UserInfoDTO createUser(UserInfoDTO pDTO, String password, String roles) {
        return UserInfoDTO.builder()
                .userName(pDTO.userName())
                .phoneNum(pDTO.phoneNum())
                .email(pDTO.email())
                .password(password)
                .roles(roles)
                .build();
    }

    /**
     * UserInfoDTO → UserInfoEntity 변환 (DB 저장용)
     * userId 는 AUTO_INCREMENT 이므로 제외
     */
    public static UserInfoEntity of(UserInfoDTO dto) {
        return UserInfoEntity.builder()
                .userName(dto.userName())
                .phoneNum(dto.phoneNum())
                .email(dto.email())
                .password(dto.password())
                .build();
    }

    /**
     * UserInfoEntity → UserInfoDTO 변환 (서비스 반환용)
     * roles 는 Entity 에 없으므로 별도로 세팅 필요 시 toBuilder() 활용
     */
    public static UserInfoDTO from(UserInfoEntity entity) {
        return UserInfoDTO.builder()
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .phoneNum(entity.getPhoneNum())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .regDt(entity.getRegDt())
                .build();
    }
}
