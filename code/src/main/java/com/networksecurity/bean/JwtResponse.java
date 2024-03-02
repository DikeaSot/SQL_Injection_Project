package com.networksecurity.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@Getter
@ToString
public class JwtResponse {

    private final String jwtToken;

}