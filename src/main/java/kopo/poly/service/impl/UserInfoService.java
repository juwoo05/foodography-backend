package kopo.poly.service.impl;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {

    private final UserInfoRepository userInfoRepository;

    private final IMailService mailService;

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
    public int sendEmailAuthCode(@NonNull UserInfoDTO pDTO,
                                 HttpSession session) throws Exception {
        log.info("{}.sendEmailAuthCode Start!", this.getClass().getName());

        int res = 0;
        int authNumber = 0;
        String email;

        authNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        email = EncryptUtil.decAES128CBC(CmmUtil.nvl(pDTO.email()));
        log.info("authNumber : {}", authNumber);

        MailDTO dto = MailDTO.builder()
                .title("이메일 중복 확인 인증번호 발송 메일")
                .contents("인증번호 " + authNumber + " 입니다.")
                .toMail(email)
                .build();

        res = mailService.doSendMail(dto);

        session.setAttribute("AUTH_NUMBER", String.valueOf(authNumber));
        session.setAttribute("AUTH_EMAIL", email);

        log.info("{}.sendEmailAuthCode End!", this.getClass().getName());

        return res;
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
