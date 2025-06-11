package com.autocoin.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class EnvironmentUtil {

    private final Environment environment;

    public EnvironmentUtil(Environment environment) {
        this.environment = environment;
        logActiveProfiles();
    }

    public String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        return environment.getDefaultProfiles()[0];
    }

    public boolean isLocalProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("local");
    }

    public boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    public boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private void logActiveProfiles() {
        log.info("==================================================");
        log.info("Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("==================================================");
    }
}
