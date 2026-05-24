package com.edunac.mentora.config;

import com.edunac.mentora.service.learning.NodeContentStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String nodeContentsUploadDir;

    public WebConfig(
            AuthInterceptor authInterceptor,
            @Value("${mentora.upload.node-contents-dir:uploads/node-contents}") String nodeContentsUploadDir
    ) {
        this.authInterceptor = authInterceptor;
        this.nodeContentsUploadDir = nodeContentsUploadDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/admin/**", "/lecturer/**", "/student/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(nodeContentsUploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler(NodeContentStorageService.PUBLIC_PREFIX + "**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
