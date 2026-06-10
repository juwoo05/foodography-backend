package kopo.poly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:8080",
                        "http://52.79.63.159:8080",
                        "https://ridinggoat.kr",
                        "http://ridinggoat.kr"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 모든 경로(/**) 중 특정 확장자가 없는 요청을 index.html로 포워딩
        registry.addViewController("/{path:[^\\.]*}")
                .setViewName("forward:/index.html");

        // 2단계 깊이의 경로 (예: /post/1) 대응
        registry.addViewController("/{path1}/{path2:[^\\.]*}")
                .setViewName("forward:/index.html");
    }
}
