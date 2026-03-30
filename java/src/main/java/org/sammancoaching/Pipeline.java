package org.sammancoaching;

import org.sammancoaching.dependencies.Config;
import org.sammancoaching.dependencies.Emailer;
import org.sammancoaching.dependencies.Logger;
import org.sammancoaching.dependencies.Project;

public class Pipeline {
    private final Config config;
    private final Emailer emailer;
    private final Logger log;

    public Pipeline(Config config, Emailer emailer, Logger log) {
        this.config = config;
        this.emailer = emailer;
        this.log = log;
    }

    public void run(Project project) {
        boolean testsPassed = runTests(project);
        // deploy só ocorre se os testes passaram
        boolean deploySuccessful = testsPassed && deploy(project);
        sendEmailNotification(testsPassed, deploySuccessful);
    }

    private boolean runTests(Project project) {
        if (project.hasTests()) {
            if ("success".equals(project.runTests())) {
                log.info("Tests passed");
                return true;
            } else {
                log.error("Tests failed");
                return false;
            }
        } else {
            log.info("No tests");
            return true;
        }
    }

    private boolean deploy(Project project) {
        if ("success".equals(project.deploy())) {
            log.info("Deployment successful");
            return true;
        } else {
            log.error("Deployment failed");
            return false;
        }
    }

    private void sendEmailNotification(boolean testsPassed, boolean deploySuccessful) {
        if (config.sendEmailSummary()) {
            log.info("Sending email");
            if (testsPassed) {
                emailer.send(deploySuccessful ? "Deployment completed successfully" : "Deployment failed");
            } else {
                emailer.send("Tests failed");
            }
        } else {
            log.info("Email disabled");
        }
    }
}
