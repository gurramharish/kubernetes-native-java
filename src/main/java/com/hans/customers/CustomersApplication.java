package com.hans.customers;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class CustomersApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomersApplication.class, args);
    }

    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
    ApplicationRunner applicationRunner() {
        return args -> System.out.println("This will run in Kubernetes!!");
    }


    @Bean
    HealthIndicator healthIndicator() {
        return () -> Health.status("I <3 Production").build();
    }

    @Bean
    ApplicationRunner runner(CustomerRepository customerRepository) {
        return args -> {
            var names = Flux.just("Harish", "Sai", "Dhikshan", "Kevin", "John", "Jim")
                    .map(name -> new Customer(null, name))
                    .flatMap(customerRepository::save);

            names.thenMany(customerRepository.findAll())
                    .subscribe(System.out::println);
        };

    }
}

@RestController
class KubernetesProbeRestController {

    private final ApplicationContext applicationContext;

    KubernetesProbeRestController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostMapping("/down")
    void down() {
        AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN);
    }
}


@RestController
class CustomerRestController {
    private final CustomerRepository customerRepository;

    CustomerRestController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers")
    Flux<Customer> get() {
        return this.customerRepository.findAll();
    }
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer>{}

record Customer(@Id Integer id, String name){}
