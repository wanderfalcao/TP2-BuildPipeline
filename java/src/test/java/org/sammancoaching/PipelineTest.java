package org.sammancoaching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sammancoaching.dependencies.Config;
import org.sammancoaching.dependencies.Emailer;
import org.sammancoaching.dependencies.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.sammancoaching.dependencies.TestStatus.*;

class PipelineTest {

    private CapturingLogger log;
    private Config config;
    private Emailer emailer;
    private Pipeline pipeline;

    @BeforeEach
    void setUp() {
        log = new CapturingLogger();
        config = mock(Config.class);
        emailer = mock(Emailer.class);
        pipeline = new Pipeline(config, emailer, log);
    }

    @Test
    void projectWithPassingTests() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).containsExactly(
                "INFO: Tests passed",
                "INFO: Deployment successful",
                "INFO: Smoke tests passed",
                "INFO: Deployment successful",
                "INFO: Sending email"
        );
        verify(emailer).send("Deployment completed successfully");
    }

    @Test
    void projectWithFailingTests() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(FAILING_TESTS)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).containsExactly(
                "ERROR: Tests failed",
                "INFO: Sending email"
        );
        verify(emailer).send("Tests failed");
    }

    @Test
    void projectWithNoTests() {
        when(config.sendEmailSummary()).thenReturn(true);
        // projetos sem testes são tratados como se os testes tivessem passado
        Project project = Project.builder()
                .setTestStatus(NO_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).containsExactly(
                "INFO: No tests",
                "INFO: Deployment successful",
                "INFO: Smoke tests passed",
                "INFO: Deployment successful",
                "INFO: Sending email"
        );
        verify(emailer).send("Deployment completed successfully");
    }

    @Test
    void deployFailure() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).containsExactly(
                "INFO: Tests passed",
                "INFO: Deployment successful",
                "INFO: Smoke tests passed",
                "ERROR: Deployment failed",
                "INFO: Sending email"
        );
        verify(emailer).send("Deployment failed");
    }

    @Test
    void deployNotAttemptedWhenTestsFail() {
        when(config.sendEmailSummary()).thenReturn(false);
        Project project = spy(Project.builder()
                .setTestStatus(FAILING_TESTS)
                .setDeploysSuccessfully(true)
                .build());

        pipeline.run(project);

        verify(project, never()).deploy();
    }

    @Test
    void noTestsDeployFails() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(NO_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).containsExactly(
                "INFO: No tests",
                "INFO: Deployment successful",
                "INFO: Smoke tests passed",
                "ERROR: Deployment failed",
                "INFO: Sending email"
        );
        verify(emailer).send("Deployment failed");
    }

    @Test
    void emailDisabledWithPassingTests() {
        when(config.sendEmailSummary()).thenReturn(false);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("INFO: Email disabled");
        verifyNoInteractions(emailer);
    }

    @Test
    void emailDisabledWithFailingTests() {
        when(config.sendEmailSummary()).thenReturn(false);
        Project project = Project.builder()
                .setTestStatus(FAILING_TESTS)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("INFO: Email disabled");
        verifyNoInteractions(emailer);
    }

    @Test
    void emailDisabledWithNoTests() {
        when(config.sendEmailSummary()).thenReturn(false);
        Project project = Project.builder()
                .setTestStatus(NO_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("INFO: Email disabled");
        verifyNoInteractions(emailer);
    }

    @Test
    void smokeTestsPass() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(PASSING_TESTS)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains(
                "INFO: Tests passed",
                "INFO: Smoke tests passed",
                "INFO: Deployment successful"
        );
        verify(emailer).send("Deployment completed successfully");
    }

    @Test
    void smokeTestsFail() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setSmokeTestStatus(FAILING_TESTS)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("ERROR: Smoke tests failed");
        verify(emailer).send("Deployment failed");
    }

    @Test
    void smokeTestsNotDefined() {
        when(config.sendEmailSummary()).thenReturn(true);
        // setSmokeTestStatus não chamado → padrão é NO_TESTS no builder
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(true)
                .setDeploysSuccessfully(false)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("ERROR: Smoke tests not defined");
    }

    @Test
    void stagingDeployFails() {
        when(config.sendEmailSummary()).thenReturn(true);
        Project project = Project.builder()
                .setTestStatus(PASSING_TESTS)
                .setDeploysSuccessfullyToStaging(false)
                .setDeploysSuccessfully(true)
                .build();

        pipeline.run(project);

        assertThat(log.getLoggedLines()).contains("ERROR: Deployment failed");
        assertThat(log.getLoggedLines()).doesNotContain("INFO: Smoke tests passed");
    }
}
