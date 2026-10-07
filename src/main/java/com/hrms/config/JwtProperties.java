package com.hrms.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long expirationMs = 86400000;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret configuration property ('jwt.secret') must not be blank.");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration configuration property ('jwt.expiration-ms') must be greater than 0.");
        }
    }
}
