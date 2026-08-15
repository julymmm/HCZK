package com.example.backend.auth.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.backend.auth.audit.SecurityAuditService;
import com.example.backend.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 鐠併倛鐦夐崪灞惧房閺夊啩瀵岄柊宥囩枂閿涘瞼绮ㄩ弸鍕棘閼板啰鐓￠崗澶愩€嶉惄顔衡偓? *
 * <p>Spring Security 閸︺劍婀版い鍦窗娑擃厺瀵岀憰浣镐粵閸ユ稐娆㈡禍瀣剁窗</p>
 * <ul>
 *   <li>閸忔娊妫撮張宥呭缁?Session閿涘奔绻氱拠浣告倵缁旑垱妲搁弮鐘靛Ц閹?API閿?/li>
 *   <li>婢圭増妲戦崫顏冪昂閹恒儱褰涢崗顒€绱戦妴浣告憿娴滄稒甯撮崣锝呯箑妞よ崵娅ヨぐ鏇樷偓浣告憿娴滄稒甯撮崣锝呯箑妞ょ粯妲哥粻锛勬倞閸涙﹫绱?/li>
 *   <li>閹?Bearer access token 娴溿倗绮?Resource Server 閺嶏繝鐛欓敍?/li>
 *   <li>閹跺﹨顓荤拠浣搞亼鐠愩儱鎷伴弶鍐娑撳秷鍐荤紒鐔剁閸栧懓顥婇幋鎰般€嶉惄顔垮殰瀹歌京娈?JSON 闁挎瑨顕ら弽鐓庣础閵?/li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 鐎靛棛鐖滈崣顏勬躬濞夈劌鍞介妴浣烘瑜版洏鈧焦鏁肩€靛棙妞傛担璺ㄦ暏 BCrypt閿涙碑WT 閺嶏繝鐛欓梼鑸殿唽娑撳秳绱伴弻銉ョ槕閻降鈧?     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CorsConfigurationSource corsConfigurationSource,
                                           JwtResourceServerConfig jwtResourceServerConfig,
                                           SecurityAuditService securityAuditService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            logJwtFailureIfPresent(securityAuditService, request, authException.getMessage());
                            writeAuthError(response, ErrorCode.UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeAuthError(response, ErrorCode.ACCESS_DENIED))
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/knowledge/resources/*/comments").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/knowledge/resources/*/like").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/knowledge/resources/*/like").authenticated()
                        .requestMatchers("/api/auth/**", "/api/oauth/**", "/api/portal/**", "/api/knowledge/**", "/api/search/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/api/projects", "/api/projects/**", "/api/tools", "/api/tools/**", "/api/articles", "/api/articles/**", "/api/senior-shares", "/api/senior-shares/**", "/api/resources", "/api/resources/**", "/api/competitions", "/api/competitions/**", "/api/qna", "/api/qna/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/projects/*/view", "/api/resources/*/view", "/api/competitions/*/view", "/api/tools/*/view", "/api/articles/*/view", "/api/senior-shares/*/view", "/api/qna/questions/*/view", "/api/ai/shares/*/summary").permitAll()
                        .requestMatchers("/api/upload/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtResourceServerConfig.jwtAuthenticationConverter())));
        return http.build();
    }

    private void logJwtFailureIfPresent(SecurityAuditService securityAuditService, HttpServletRequest request, String reason) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }
        String token = header.substring(7);
        securityAuditService.logJwtValidationFailure(maskToken(token), request, reason == null ? "JWT authentication failed" : reason);
    }

    private String maskToken(String token) {
        return token.substring(0, Math.min(token.length(), 20)) + "...";
    }

    private void writeAuthError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", errorCode.getCode(),
                "error", errorCode.name(),
                "message", errorCode.getMessage()
        ));
    }
}
