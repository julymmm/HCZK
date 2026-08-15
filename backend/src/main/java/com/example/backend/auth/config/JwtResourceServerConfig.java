package com.example.backend.auth.config;

import com.example.backend.auth.token.JwtKeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.Locale;

/**
 * Spring Security Resource Server 的 JWT 配置。
 *
 * <p>这里负责校验每次请求里的 Authorization: Bearer accessToken：</p>
 * <ul>
 *   <li>使用 RSA 公钥校验 RS256 签名；</li>
 *   <li>校验 exp，过期令牌不能通过；</li>
 *   <li>额外要求 token_type=access，防止 refresh token 被当作访问令牌使用；</li>
 *   <li>把 JWT 中的 role 转成 Spring Security 的 ROLE_USER/ROLE_ADMIN。</li>
 * </ul>
 */
@Configuration
public class JwtResourceServerConfig {

    @Bean
    public JwtDecoder jwtDecoder(JwtKeyProvider keyProvider) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyProvider.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                accessTokenValidator()
        ));
        return decoder;
    }

    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            String role = normalizeRole(jwt.getClaimAsString("role"));
            return new JwtAuthenticationToken(
                    jwt,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))),
                    jwt.getSubject()
            );
        };
    }

    private OAuth2TokenValidator<Jwt> accessTokenValidator() {
        return jwt -> {
            String tokenType = jwt.getClaimAsString("token_type");
            if ("access".equals(tokenType)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Only access tokens can be used as bearer credentials",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    private String normalizeRole(String role) {
        return role == null || role.isBlank() ? "user" : role.trim().toLowerCase(Locale.ROOT);
    }
}
