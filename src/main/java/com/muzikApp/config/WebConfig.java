package com.muzikApp.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Uploaded songs and images are served as static files: /uploads/songs/..., /uploads/images/... */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/songs/**")
                .addResourceLocations(location(properties.storage().songDir()));
        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(location(properties.storage().imageDir()));
    }

    private static String location(String dir) {
        return Path.of(dir).toAbsolutePath().toUri().toString();
    }
}
