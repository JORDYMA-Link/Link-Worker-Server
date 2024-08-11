package com.jordyma.blink.auth.service

import com.jordyma.blink.user.entity.SocialType
import com.jordyma.blink.user.entity.User
import com.jordyma.blink.user.repository.UserRepository
import com.jordyma.blink.auth.dto.request.KakaoLoginRequestDto
import com.jordyma.blink.auth.dto.response.TokenResponseDto
import com.jordyma.blink.auth.jwt.enums.TokenType
import com.jordyma.blink.auth.jwt.util.JwtTokenUtil
import com.jordyma.blink.global.exception.ApplicationException
import com.jordyma.blink.global.exception.ErrorCode
import com.jordyma.blink.global.http.api.KakaoAuthApi
import com.jordyma.blink.global.http.request.GetKakaoTokenRequestDto
import com.jordyma.blink.user.entity.Role
import com.jordyma.blink.user_refresh_token.entity.UserRefreshToken
import com.jordyma.blink.user_refresh_token.repository.UserRefreshTokenRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwt
import lombok.RequiredArgsConstructor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
class AuthService(
    private val jwtTokenUtil: JwtTokenUtil,
    private val userRepository: UserRepository,
    private val kakaoAuthApi: KakaoAuthApi,
    private val userRefreshTokenRepository: UserRefreshTokenRepository
) {

    @Value("\${kakao.auth.jwt.aud}")
    val aud: String? = null

    @Value("\${kakao.auth.jwt.iss}")
    val iss: String? = null

    @Value("\${kakao.auth.jwt.client-id}")
    lateinit var kakaoClientId: String

    @Value("\${kakao.auth.jwt.redirect-uri}")
    lateinit var kakaoRedirectUri: String

    @Transactional
    fun kakaoLogin(kakaoLoginRequestDto: KakaoLoginRequestDto): TokenResponseDto {
        val idToken: String = kakaoLoginRequestDto.idToken;
        val claims: Jwt<Header<*>, Claims> = jwtTokenUtil.parseJwt(idToken)

        val kid: String = claims.header.get("kid").toString()
        jwtTokenUtil.verifySignature(idToken, kid, aud, iss, kakaoLoginRequestDto.nonce)

        val nickname: String = claims.body.get("nickname", String::class.java)
        val socialUserId: String = claims.body.get("sub", String::class.java)

        val user: User = upsertUser(SocialType.KAKAO, socialUserId, nickname)

        val accessToken = jwtTokenUtil.generateToken(TokenType.ACCESS_TOKEN, user)
        val refreshToken = jwtTokenUtil.generateToken(TokenType.REFRESH_TOKEN, user)

        userRefreshTokenRepository.save(UserRefreshToken.of(refreshToken, user))

        return TokenResponseDto(accessToken, refreshToken);
    }

    private fun upsertUser(socialType: SocialType, socialUserId: String, nickname: String): User {
        return userRepository.findBySocialTypeAndSocialUserId(socialType, socialUserId)
            ?: userRepository.save(
                User(
                    nickname = nickname,
                    socialType = SocialType.KAKAO,
                    socialUserId = socialUserId,
                    role = Role.USER
                )
            )
    }

    @Transactional
    fun regenerateToken(token: String?): TokenResponseDto {
        val claims = jwtTokenUtil.parseToken(token)
        val tokenType: TokenType = TokenType.valueOf(claims!!.body["type", String::class.java])

        if (tokenType !== TokenType.REFRESH_TOKEN) {
            throw ApplicationException(ErrorCode.TOKEN_VERIFICATION_EXCEPTION, "올바른 토큰 타입이 아닙니다.")
        }

        val userRefreshToken: UserRefreshToken = userRefreshTokenRepository.findByRefreshToken(token)
            ?: throw ApplicationException(ErrorCode.TOKEN_VERIFICATION_EXCEPTION, "올바르지 않은 토큰입니다.")
        val subject = claims.body.subject
        val user: User = userRepository.findById(subject.toLong())
            .orElseThrow { ApplicationException(ErrorCode.TOKEN_VERIFICATION_EXCEPTION, "올바르지 않은 토큰입니다.") }
        val accessToken = jwtTokenUtil.generateToken(TokenType.ACCESS_TOKEN, user)
        val refreshToken = jwtTokenUtil.generateToken(TokenType.REFRESH_TOKEN, user)

        userRefreshToken.updateRefreshToken(refreshToken)

        return TokenResponseDto(accessToken, refreshToken)
    }

    @Transactional
    fun kakaoLoginWeb(code: String): TokenResponseDto {

        val redirectUri = this.kakaoRedirectUri
        val tokenResponse = kakaoAuthApi.getKakaoToken(this.kakaoClientId, redirectUri, code)

        val idToken = tokenResponse.id_token
        val claims: Jwt<Header<*>, Claims> = jwtTokenUtil.parseJwt(idToken)
        val kid: String = claims.header.get("kid").toString()
        jwtTokenUtil.verifySignature(idToken, kid, this.kakaoClientId, iss, null)

        val nickname: String = claims.body.get("nickname", String::class.java)
        val socialUserId: String = claims.body.get("sub", String::class.java)

        val user: User = upsertUser(SocialType.KAKAO, socialUserId, nickname)

        val accessToken = jwtTokenUtil.generateToken(TokenType.ACCESS_TOKEN, user)
        val refreshToken = jwtTokenUtil.generateToken(TokenType.REFRESH_TOKEN, user)

        userRefreshTokenRepository.save(UserRefreshToken.of(refreshToken, user))

        return TokenResponseDto(accessToken, refreshToken)
    }
}