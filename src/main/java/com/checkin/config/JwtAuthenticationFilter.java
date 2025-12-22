// src/main/java/com/checkin/config/JwtAuthenticationFilter.java
package com.checkin.config;

import com.checkin.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // ========== 核心修改：新增Swagger路径放行逻辑（放在方法最开头） ==========
        String requestURI = request.getRequestURI();
        // 判断是否是Swagger相关路径，若是则直接放行，不执行JWT校验
        if (requestURI.contains("/v3/api-docs") ||
                requestURI.contains("/swagger-ui") ||
                requestURI.contains("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return; // 跳过后续JWT校验逻辑，直接放行
        }

        // ========== 原有JWT校验逻辑（保留不变） ==========
        try {
            // 从请求头获取令牌
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7); // 截取"Bearer "后的令牌
                username = jwtUtils.extractUsername(token);
            }

            // 验证令牌并设置认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtils.validateToken(token, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username, null, null
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