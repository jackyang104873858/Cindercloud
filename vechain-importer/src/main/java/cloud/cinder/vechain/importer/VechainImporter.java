package cloud.cinder.vechain.importer;

import cloud.cinder.common.CindercloudCommon;
import cloud.cinder.common.infrastructure.IgnoreDuringComponentScan;
import cloud.cinder.vechain.CindercloudVechain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.InfoContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EntityScan(basePackageClasses = {
        CindercloudCommon.class,
        CindercloudVechain.class,
        VechainImporter.class
})
@ComponentScan(
        basePackageClasses = {
                CindercloudCommon.class,
                CindercloudVechain.class,
                VechainImporter.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(IgnoreDuringComponentScan.class)})
@EnableAutoConfiguration(exclude = {
        EndpointAutoConfiguration.class,
        HealthIndicatorAutoConfiguration.class,
        InfoContributorAutoConfiguration.class
})
@Slf4j
public class VechainImporter {

    public static void main(String[] args) {
        final SpringApplication app = new SpringApplication(VechainImporter.class);
        final Environment env = app.run(args).getEnvironment();
        log.info("\n----------------------------------------------------------\n\t"
                        + "Application '{}' is running! Access URLs:\n\t"
                        + "Profile(s): \t{}\n----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getActiveProfiles());
    }
}