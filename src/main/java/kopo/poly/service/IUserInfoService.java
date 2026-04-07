package kopo.poly.service;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.UserInfoDTO;

public interface IUserInfoService {

    UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception;

    UserInfoDTO searchUserEmail(UserInfoDTO pDTO) throws Exception;

    int sendEmailAuthCode(UserInfoDTO pDTO, HttpSession session) throws Exception;

    int insertUserInfo(UserInfoDTO pDTO) throws Exception;

    int getUserLogin(UserInfoDTO pDTO) throws Exception;
}
