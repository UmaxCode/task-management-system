package org.umaxcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.umaxcode.controller.PingController;
import org.umaxcode.controller.TaskManagementController;
import org.umaxcode.controller.UserAuthController;


@SpringBootApplication
// We use direct @Import instead of @ComponentScan to speed up cold starts
// @ComponentScan(basePackages = "org.umaxcode.controller")
@Import({PingController.class, TaskManagementController.class, UserAuthController.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}