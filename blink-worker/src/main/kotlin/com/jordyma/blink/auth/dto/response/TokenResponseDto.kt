package com.jordyma.blink.auth.dto.response

data class TokenResponseDto (
//    @Schema(description = "액세스 토큰")
    val accessToken: String,

//    @Schema(description = "리프레시 토큰")
    val refreshToken: String,
    )