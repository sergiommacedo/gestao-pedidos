package br.com.sergio.gestaopedidos.config;

import br.com.sergio.gestaopedidos.security.TrocaSenhaObrigatoriaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TrocaSenhaObrigatoriaInterceptor trocaSenhaObrigatoriaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(trocaSenhaObrigatoriaInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/alterar-senha-inicial",
                        "/logout",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/webjars/**"
                );
    }
}
