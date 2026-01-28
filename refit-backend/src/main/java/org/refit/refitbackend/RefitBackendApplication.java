package org.refit.refitbackend;

import org.refit.refitbackend.domain.auth.config.properties.OAuth2ProviderProperties;
import org.refit.refitbackend.domain.auth.config.properties.OAuth2RegistrationProperties;
import org.refit.refitbackend.global.storage.aws.AwsS3ClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties({
    OAuth2ProviderProperties.class,
    OAuth2RegistrationProperties.class,
    AwsS3ClientProperties.class
})
@SpringBootApplication
public class RefitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefitBackendApplication.class, args);
    }

}
