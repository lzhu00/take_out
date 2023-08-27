package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.R;
import org.example.reggie.entity.User;
import org.example.reggie.service.UserService;
import org.example.reggie.utils.MailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) throws MessagingException {
        String phone = user.getPhone();

        if (phone.isEmpty()){
            return R.error("未收到邮箱");
        }

        String code = MailUtils.achieveCode();
        log.info(code);

        MailUtils.sendTestMail(phone, code);
        session.setAttribute(phone, code);

        return R.success("验证码发送成功");
    }

    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();

        String userCode = session.getAttribute(phone).toString();

        if (userCode != null && userCode.equals(code)){
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);

            User user = userService.getOne(queryWrapper);

            if (user == null){
                user = new User();
                user.setPhone(phone);

                userService.save(user);
            }
            session.setAttribute("user",user.getId());//添加登录信息，进入filter
            return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> logout(HttpServletRequest servletRequest){
        servletRequest.getSession().removeAttribute("user");
        return R.success("登出账号");
    }
}
