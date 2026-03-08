// src/main/java/com/checkin/config/JwtAuthenticationFilter.java
package com.checkin.config;

import com.checkin.service.UserDetailsServiceImpl;
import com.checkin.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    // 核心新增：注入UserDetailsService，用于加载完整的用户信息
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // ========== Swagger路径放行逻辑（保留） ==========
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/v3/api-docs") ||
                requestURI.contains("/swagger-ui") ||
                requestURI.contains("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ========== 修复后的JWT校验逻辑 ==========
        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                username = jwtUtils.extractUsername(token);
            }

            // 验证令牌并设置认证信息（核心修改）
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 1. 加载完整的UserDetails对象（不再用字符串）
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 2. 验证token有效性（注意：这里要适配你的JwtUtils方法，若参数是UserDetails则改传userDetails）
                if (jwtUtils.validateToken(token, username)) {
                    // 3. 构建正确的认证对象：传入UserDetails而非字符串
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,  // 核心修改：用UserDetails代替String
                            null,
                            userDetails.getAuthorities() // 补充用户权限（必填）
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("JWT认证失败: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}