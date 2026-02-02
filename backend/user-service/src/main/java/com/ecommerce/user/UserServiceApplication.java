package com.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * USER SERVICE APPLICATION
 * 
 * Point d'entrée de l'application Spring Boot
 * 
 * @SpringBootApplication active :
 * - @Configuration : Configuration Spring
 * - @EnableAutoConfiguration : Configuration automatique
 * - @ComponentScan : Scan des composants (@Service, @Controller, @Repository, etc.)
 * 
 * Pour démarrer :
 * mvn spring-boot:run
 * ou
 * java -jar target/user-service-1.0.0.jar
 */
@SpringBootApplication
@EnableMongoAuditing  // Active l'auditing MongoDB (createdAt, updatedAt auto)
public class UserServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("🚀 User Service démarré sur http://localhost:8081");
    }
}
