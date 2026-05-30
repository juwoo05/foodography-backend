package kopo.poly.service.impl;

import jakarta.transaction.Transactional;
import kopo.poly.auth.AuthInfo;
import kopo.poly.dto.MailDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.repository.UserInfoRepository;
import kopo.poly.repository.entity.UserInfoEntity;
import kopo.poly.service.IMailService;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {

    // Redis 인증코드 키 접두사 · TTL
    private static final String AUTH_CODE_PREFIX  = "AUTH:EMAIL:";
    private static final Duration AUTH_CODE_TTL   = Duration.ofMinutes(5);

    private final UserInfoRepository userInfoRepository;
    private final IMailService mailService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Spring Security 인증 진입점 — AuthenticationManager.authenticate() 가 내부 호출
     *
     * 파라미터 email : 컨트롤러에서 AES-128 암호화된 이메일을 전달
     *   → DB 조회 키와 동일한 형태 (암호화 저장)
     *
     * JWT sub 클레임은 userId(Integer) 로 발급 — PII(이메일) 노출 방지
     * jwt 검증은 JwtDecoder 가 처리 — 이 메서드는 로그인 시에만 호출됨
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.info("{}.loadUserByUsername Start! email={}", this.getClass().getName(), email);

        UserInfoEntity entity = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일: " + email));

        // Entity 에 roles 컬럼 없음 → 기본값 ROLE_USER 부여
        UserInfoDTO rDTO = UserInfoDTO.builder()
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .phoneNum(entity.getPhoneNum())
                .email(entity.getEmail())
                .password(entity.getPassword())   // SHA-256 해시 — PasswordEncoder.matches() 비교용
                .regDt(entity.getRegDt())
                .roles("ROLE_USER")
                .build();

        log.info("{}.loadUserByUsername End! userId={}", this.getClass().getName(), entity.getUserId());

        return new AuthInfo(rDTO);
    }

    @Override
    public UserInfoDTO getUserEmailExists(@NonNull UserInfoDTO pDTO) throws Exception {
        log.info("{}.getUserEmailExists Start!", this.getClass().getName());

        log.info("pDTO : {}", pDTO);

        String email = CmmUtil.nvl(pDTO.email());

        boolean exists = userInfoRepository.findByEmail(email).isPresent();

        String existYn = exists ? "Y" : "N";

        UserInfoDTO rDTO = UserInfoDTO.builder()
                .existYn(existYn)
                .build();

        log.info("{}.getUserEmailExists End!", this.getClass().getName());

        return rDTO;
    }

    @Override
    public int sendEmailAuthCode(@NonNull UserInfoDTO pDTO) throws Exception {
        log.info("{}.sendEmailAuthCode Start!", this.getClass().getName());

        String email      = EncryptUtil.decAES128CBC(CmmUtil.nvl(pDTO.email()));
        int    authNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);

        log.info("authNumber : {}", authNumber);

        MailDTO mailDTO = MailDTO.builder()
                .title("이메일 중복 확인 인증번호 발송 메일")
                .contents("인증번호 " + authNumber + " 입니다.")
                .toMail(email)
                .build();

        int res = mailService.doSendMail(mailDTO);

        if (res == 1) {
            // HttpSession 대신 Redis TTL 5분 저장 — 서버 재시작·스케일아웃에도 안전
            stringRedisTemplate.opsForValue()
                    .set(AUTH_CODE_PREFIX + email, String.valueOf(authNumber), AUTH_CODE_TTL);
            log.info("인증코드 Redis 저장 완료 | email={} | ttl={}m", email, AUTH_CODE_TTL.toMinutes());
        }

        log.info("{}.sendEmailAuthCode End!", this.getClass().getName());

        return res;
    }

    @Override
    public int verifyEmailCode(String email, String inputCode) {
        log.info("{}.verifyEmailCode Start! email={}", this.getClass().getName(), email);

        String savedCode = CmmUtil.nvl(stringRedisTemplate.opsForValue().get(AUTH_CODE_PREFIX + email));

        if (!savedCode.isEmpty() && savedCode.equals(inputCode)) {
            stringRedisTemplate.delete(AUTH_CODE_PREFIX + email); // 1회성 보장
            log.info("인증 성공 | email={}", email);
            return 1;
        }

        log.info("인증 실패 | email={} | savedCode존재={}", email, !savedCode.isEmpty());
        return 0;
    }

    @Override
    public int insertUserInfo(@NonNull UserInfoDTO pDTO) throws Exception {

        log.info("{}.insertUserInfo Start!", this.getClass().getName());

        log.info("pDTO : {}", pDTO);

        int res;

        String userName = CmmUtil.nvl(pDTO.userName()); // 이름
        String phoneNum = CmmUtil.nvl(pDTO.phoneNum());
        String email = CmmUtil.nvl(pDTO.email()); // 이메일
        String password = CmmUtil.nvl(pDTO.password()); // 비밀번호

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByEmail(email);

        if (rEntity.isPresent()) {
            res = 2;

        } else {

            // 회원가입을 위한 Entity 생성
            UserInfoEntity pEntity = UserInfoEntity.builder()
                    .userName(userName)
                    .phoneNum(phoneNum)
                    .email(email)
                    .password(password)
                    .build();

            userInfoRepository.save(pEntity);

            res = userInfoRepository.findByEmail(email).isPresent() ? 1 : 0;

        }

        log.info("{}.insertUserInfo End!", this.getClass().getName());

        return res;
    }

    @Override
    public int getUserLogin(@NonNull UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserLoginCheck Start!", this.getClass().getName());

        String email = CmmUtil.nvl(pDTO.email());
        String password = CmmUtil.nvl(pDTO.password());

        log.info("email : {}, password : {}", email, password);

        boolean res = userInfoRepository.findByEmailAndPassword(email, password).isPresent();

        log.info("{}.getUserLoginCheck End!", this.getClass().getName());

        return res ? 1 : 0;
    }

    @Override
    public UserInfoDTO searchUserEmail(@NonNull UserInfoDTO pDTO) throws Exception {

        log.info("{}.searchUserEmail Start!", this.getClass().getName());

        String userName = CmmUtil.nvl(pDTO.userName());
        String phoneNum = CmmUtil.nvl(pDTO.phoneNum());

        log.info("userName : {}, phoneNum : {}", userName, phoneNum);

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByUserNameAndPhoneNum(userName, phoneNum);

        UserInfoDTO rDTO = null;

        if (rEntity.isPresent()) {
            UserInfoEntity entity = rEntity.get();

            String email = EncryptUtil.decAES128CBC(entity.getEmail());

            rDTO = UserInfoDTO.builder()
                    .userName(entity.getUserName())
                    .phoneNum(entity.getPhoneNum())
                    .email(email)
                    .build();
        }

        log.info("{}.searchUserEmail End!", this.getClass().getName());

        return rDTO;
    }

    @Override
    @Transactional
    public int updatePassword(@NonNull UserInfoDTO pDTO) throws Exception {

        log.info("{}.updatePassword Start!", this.getClass().getName());

        int res = 0;

        String email = CmmUtil.nvl(pDTO.email());
        String password = CmmUtil.nvl(pDTO.password());

        Optional<UserInfoEntity> rEntity = userInfoRepository.findByEmail(email);

        if (rEntity.isPresent()) {

            UserInfoEntity entity = rEntity.get();

            if (entity.getPassword().equals(password)) {

                res = 2;
                log.info("비밀번호 변경 실패: 이전 비밀번호와 동일함");

            } else {

                entity.updatePassword(password);

                res = 1;
                log.info("비밀번호 변경 성공: {}", password);

            }

        } else {
            // 사용자가 존재하지 않는 경우
            log.info("비밀번호 변경 실패: 존재하지 않는 이메일 ({})", email);
        }

        log.info("{}.updatePassword End!", this.getClass().getName());

        return res;
    }
}
