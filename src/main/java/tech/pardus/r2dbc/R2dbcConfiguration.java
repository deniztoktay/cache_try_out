package tech.pardus.r2dbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "tech.pardus.r2dbc")
public class R2dbcConfiguration {}
