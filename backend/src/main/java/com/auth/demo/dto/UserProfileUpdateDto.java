package com.auth.demo.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String name;
    private String bio;
    private String photoUrl;
}
