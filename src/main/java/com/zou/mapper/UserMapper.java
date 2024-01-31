package com.zou.mapper;

import com.zou.pojo.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class UserMapper {

    private List<User> userList = Arrays.asList(
            new User("1", "leonard", "15073323286", "star1", "1111"),
            new User("2", "suga", "15069888432", "star2", "2222"),
            new User("3", "bob", "16789905356", "star3", "3333"),
            new User("4", "nike", "88785679", "star4", "4444")
    );

    public User getUserFromPhoneNumber(String phonenumber) {
        return userList.stream()
                .filter(user -> user.getPhone().equals(phonenumber))
                .findFirst().orElse(null);
    }

}
