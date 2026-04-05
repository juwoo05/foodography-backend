package kopo.poly.service;

import kopo.poly.dto.MailDTO;

public interface IMailService {

    int doSendMail(MailDTO pDTO);
}
