package com.checkin.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // 生成签名密钥（使用更安全的方式处理密钥）
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成包含自定义声明的token（支持角色信息）
    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        // 存储用户角色信息
        claims.put("roles", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return createToken(claims, username);
    }

    // 简化版生成token（默认无额外声明）
    public String generateToken(String username) {
        return generateToken(username, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // 创建token核心方法
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 使用安全密钥对象
                .compact();
    }

    // 刷新令牌（基于旧令牌生成新令牌，延长有效期）
    public String refreshToken(String oldToken) {
        if (!StringUtils.hasText(oldToken)) {
            return null;
        }
        try {
            Claims claims = extractAllClaims(oldToken);
            // 保留原有声明，更新签发时间和过期时间
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            return null;
        }
    }

    // 从token中获取用户名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 从token中获取角色信息
    public List<GrantedAuthority> extractRoles(String token) {
        List<String> roles = extractClaim(token, claims -> (List<String>) claims.get("roles"));
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // 从token中获取声明
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            // 提取失败时返回null，由调用方处理
            return null;
        }
    }

    // 验证token是否有效（增强异常处理）
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // 捕获所有JWT相关异常（无效签名、过期、格式错误等）
            return false;
        }
    }

    // 验证token与用户名是否匹配
    public boolean validateToken(String token, String username) {
        return validateToken(token) && extractUsername(token).equals(username);
    }

    // 检查token是否过期
    public boolean isTokenExpired(String token) {
        Date expirationDate = extractExpiration(token);
        return expirationDate != null && expirationDate.before(new Date());
    }

    // 从token中获取过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 解析token获取所有声明（增加异常捕获）
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 令牌已过期，返回过期的claims
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            // 其他异常（无效签名、格式错误等）
            throw new IllegalArgumentException("无效的JWT令牌: " + e.getMessage());
        }
    }

    // 从请求中获取token（优化空值处理）
    public String getTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim(); // 去除可能的空格
        }
        return null;
    }
}