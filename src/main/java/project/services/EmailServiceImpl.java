package project.services;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * Для отправки почты воспользуемся классом JavaMailSender, для конфигурирования которого создадми класс MailConfig
 */

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;



    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String userName;

    @SneakyThrows
    @Override
    public void send(String mailTo, String subject, String text) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        message.setFrom(userName);
        message.setTo(mailTo);
        message.setSubject(subject);
        message.setText(text, true);

        mailSender.send(mimeMessage);
    }
}
