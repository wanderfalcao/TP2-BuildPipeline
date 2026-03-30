package org.sammancoaching;

import org.sammancoaching.dependencies.Config;
import org.sammancoaching.dependencies.DeploymentEnvironment;
import org.sammancoaching.dependencies.Emailer;
import org.sammancoaching.dependencies.Logger;
import org.sammancoaching.dependencies.Project;
import org.sammancoaching.dependencies.TestStatus;

public class Pipeline {
    private static final String SUCCESS = "success";

    private final Config config;
    private final Emailer emailer;
    private final Logger log;

    public Pipeline(Config config, Emailer emailer, Logger log) {
        this.config = config;
        this.emailer = emailer;
        this.log = log;
    }

    /**
     * Executa o pipeline: roda testes, faz deploy em staging, valida smoke tests,
     * faz deploy em produção e envia notificação por email.
     */
    public void run(Project project) {
        PipelineResult result = buildResult(project);
        sendEmailNotification(result);
    }

    private PipelineResult buildResult(Project project) {
        boolean testsPassed = runTests(project);
        if (!testsPassed) {
            return new PipelineResult(false, false);
        }

        boolean stagingOk = deployToEnvironment(project, DeploymentEnvironment.STAGING);
        if (!stagingOk) {
            return new PipelineResult(true, false);
        }

        boolean smokeOk = runSmokeTests(project);
        if (!smokeOk) {
            return new PipelineResult(true, false);
        }

        boolean prodOk = deployToEnvironment(project, DeploymentEnvironment.PRODUCTION);
        return new PipelineResult(true, prodOk);
    }

    private boolean runTests(Project project) {
        if (project.hasTests()) {
            if (SUCCESS.equals(project.runTests())) {
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

    private boolean deployToEnvironment(Project project, DeploymentEnvironment env) {
        if (SUCCESS.equals(project.deploy(env))) {
            log.info("Deployment successful");
            return true;
        } else {
            log.error("Deployment failed");
            return false;
        }
    }

    private boolean runSmokeTests(Project project) {
        TestStatus status = project.runSmokeTests();
        switch (status) {
            case PASSING_TESTS:
                log.info("Smoke tests passed");
                return true;
            case FAILING_TESTS:
                log.error("Smoke tests failed");
                return false;
            default:
                log.error("Smoke tests not defined");
                return false;
        }
    }

    private void sendEmailNotification(PipelineResult result) {
        if (config.sendEmailSummary()) {
            log.info("Sending email");
            if (result.testsPassed()) {
                emailer.send(result.deploySuccessful() ? "Deployment completed successfully" : "Deployment failed");
            } else {
                emailer.send("Tests failed");
            }
        } else {
            log.info("Email disabled");
        }
    }
}
