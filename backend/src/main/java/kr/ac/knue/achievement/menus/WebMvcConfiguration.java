package kr.ac.knue.achievement.menus;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final MenuAuthorizationInterceptor menuAuthorizationInterceptor;

    public WebMvcConfiguration(MenuAuthorizationInterceptor menuAuthorizationInterceptor) {
        this.menuAuthorizationInterceptor = menuAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(menuAuthorizationInterceptor).addPathPatterns("/api/**");
    }
}
