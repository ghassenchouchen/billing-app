package com.telecom.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class UsageServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UsageServiceApplication.class, args);
    }
}
