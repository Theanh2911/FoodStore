package yenha.foodstore.Auth.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:[*]");

        config.addAllowedOriginPattern("https://*.vercel.app");

        config.addAllowedOriginPattern("https://yenhafood.site");
        config.addAllowedOriginPattern("https://www.yenhafood.site");
        config.addAllowedOriginPattern("https://admin.yenhafood.site");

        config.addAllowedMethod("*");

        config.addAllowedHeader("*");

        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
