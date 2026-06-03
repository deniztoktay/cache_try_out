package tech.pardus.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(DataSourceProperties.class)
@EntityScan(basePackages = "tech.pardus.jdbc")
@EnableJpaRepositories(basePackages = "tech.pardus.jdbc")
public class JdbcConfig {

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  HikariDataSource dataSource(DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  TransactionTemplate jpaTransactionTemplate(PlatformTransactionManager transactionManager) {
    var template = new TransactionTemplate(transactionManager);
    template.setReadOnly(false);
    return template;
  }

  @Bean(name = "jdbcThreadPool", destroyMethod = "shutdown")
  ThreadPoolTaskExecutor jdbcThreadPool(HikariDataSource dataSource) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(dataSource.getMaximumPoolSize());
    executor.setQueueCapacity(1000);
    executor.setThreadNamePrefix("jdbc-writer-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    executor.initialize();
    return executor;
  }
}
