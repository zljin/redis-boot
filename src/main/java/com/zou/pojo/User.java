package com.zou.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author leonard
 * @date 2022/11/10
 * @Description TODO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    private String id;
    private String nickName;
    private String phone;
    private String icon;
    private String password;
}
