package com.networksecurity.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Getter
@Setter
@ToString
public class JwtRequest {

    private String username;
    private String password;
}