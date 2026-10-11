package kopo.poly.auth;

import kopo.poly.dto.UserInfoDTO;
import kopo.poly.util.CmmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public record AuthInfo(UserInfoDTO userInfoDTO) implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<GrantedAuthority> pSet = new HashSet<>();

        String roles = CmmUtil.nvl(userInfoDTO.roles());

        if (!roles.isEmpty()) {
            for (String role : roles.split(",")) {
                pSet.add(new SimpleGrantedAuthority(role));
            }
        }

        return pSet;
    }

    @Override
    public String getPassword() {
        return CmmUtil.nvl(userInfoDTO.password());
    }

    /**
     * Spring Security principal 식별자 → JWT sub 클레임으로 사용됨
     * email 대신 userId(Integer) 를 String 으로 변환해 사용:
     *   - JWT payload 에 PII(이메일) 노출 방지
     *   - 이메일 변경 시에도 토큰 주체 불변
     */
    @Override
    public String getUsername() {
        return String.valueOf(userInfoDTO.userId());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
