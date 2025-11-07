package com.lfs.domain;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String phoneNum;

}
