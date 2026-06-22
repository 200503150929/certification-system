package com.certification.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成 Token、解析 Claims、验证过期时间
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;

    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:certification-system-secret-key-2024-eeas-jwt-hs256}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用户名
     * @param role     用户角色
     * @return JWT token 字符串
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 中解析 Claims
     *
     * @param token JWT token
     * @return Claims，解析失败返回 null
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token");
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误");
        } catch (Exception e) {
            log.warn("JWT Token 解析失败", e);
        }
        return null;
    }

    /**
     * 从 Token 中提取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从 Token 中提取角色
     */
    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        return claims != null && !claims.getExpiration().before(new Date());
    }
}