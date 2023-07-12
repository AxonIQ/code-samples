package io.axoniq.dev.samples;

import org.axonframework.config.Configuration;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.SimpleDeadlineManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SagaNoToastApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagaNoToastApplication.class);
    }

    /**
     * We're using a {@link SimpleDeadlineManager} to fulfill the requirements of the
     * {@link io.axoniq.dev.samples.saga.ProcessOrderSaga}. Note though that you should not regard this
     * {@link DeadlineManager} implementation as production-ready, as it does not persist scheduled deadlines!
     */
    @Bean
    public DeadlineManager deadlineManager(Configuration configuration) {
        return SimpleDeadlineManager.builder()
                                    .scopeAwareProvider(configuration.scopeAwareProvider())
                                    .build();
    }
}
