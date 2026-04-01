package kopo.poly.service.impl;

import kopo.poly.dto.UserInfoDTO;
import kopo.poly.repository.UserInfoRepository;
import kopo.poly.repository.entity.UserInfoEntity;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.DateUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {

    private final UserInfoRepository userInfoRepository;

    @Override
    public UserInfoDTO getUserEmailExists(@NonNull UserInfoDTO pDTO) throws Exception {
        log.info("{}.getUserEmailExists Start!", this.getClass().getName());

        log.info("pDTO : {}", pDTO);

        String email = CmmUtil.nvl(pDTO.email());

        boolean exists = userInfoRepository.findByEmail(email).isPresent();

        UserInfoDTO rDTO = UserInfoDTO.builder()
                .existYn(exists ? "Y" : "N")
                .build();

        log.info("{}.getUserEmailExists End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public int insertUserInfo(@NonNull UserInfoDTO pDTO) throws Exception {

        log.info("{}.inserUserInfo Start!", this.getClass().getName());

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
}
