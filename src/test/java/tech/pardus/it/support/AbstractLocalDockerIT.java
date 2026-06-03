package tech.pardus.it.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tech.pardus.App;

/** Base for integration tests targeting local docker-compose services. */
@SpringBootTest(classes = App.class)
@ActiveProfiles("it")
public abstract class AbstractLocalDockerIT {}
