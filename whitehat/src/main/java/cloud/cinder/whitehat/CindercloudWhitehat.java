package cloud.cinder.whitehat;

import cloud.cinder.common.CindercloudCommon;
import cloud.cinder.common.infrastructure.IgnoreDuringComponentScan;
import cloud.cinder.ethereum.CindercloudEthereum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EntityScan(basePackageClasses = {
        CindercloudCommon.class,
        CindercloudEthereum.class,
        CindercloudWhitehat.class
})
@ComponentScan(
        basePackageClasses = {
                CindercloudCommon.class,
                CindercloudEthereum.class,
                CindercloudWhitehat.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(IgnoreDuringComponentScan.class)})
@Slf4j
public class CindercloudWhitehat {

    public static void main(String[] args) {
        final SpringApplication app = new SpringApplication(CindercloudWhitehat.class);
        final Environment env = app.run(args).getEnvironment();
        log.info("\n----------------------------------------------------------\n\t"
                        + "Application '{}' is running! Access URLs:\n\t"
                        + "Profile(s): \t{}\n----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getActiveProfiles());
    }
}
