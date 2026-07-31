package br.com.sergio.gestaopedidos.config;

import br.com.sergio.gestaopedidos.security.TrocaSenhaObrigatoriaInterceptor;
import br.com.sergio.gestaopedidos.service.LogoEmpresaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TrocaSenhaObrigatoriaInterceptor trocaSenhaObrigatoriaInterceptor;
    private final LogoEmpresaStorageService logoEmpresaStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/logos/**")
                .addResourceLocations(logoEmpresaStorageService.obterLocalizacaoRecurso());
    }

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
                        "/uploads/**",
                        "/webjars/**"
                );
    }
}
