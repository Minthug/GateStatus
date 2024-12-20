package com.example.GateStatus.global.kubernetes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
@Getter
@Setter
public class KubernetesProperties {

    private String dbHost;
    private String dbPort;
    private String redisHost;
    private String redisPort;
}
