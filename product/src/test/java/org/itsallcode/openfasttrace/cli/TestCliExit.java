package org.itsallcode.openfasttrace.cli;

import static org.itsallcode.junit.sysextensions.AssertExit.assertExitWithStatus;

import java.time.Duration;
import java.util.*;

import org.itsallcode.junit.sysextensions.ExitGuard;
import org.itsallcode.openfasttrace.core.cli.*;
import org.itsallcode.openfasttrace.core.cli.commands.TraceCommand;
import org.itsallcode.openfasttrace.testutil.TestAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("removal") // https://github.com/itsallcode/openfasttrace/issues/436
@ExtendWith(ExitGuard.class)
class TestCliExit
{
    private static final String TEST_RESOURCES_MARKDOWN = "../core/src/test/resources/markdown";
    private static final String SAMPLE_DESIGN = TEST_RESOURCES_MARKDOWN + "/sample_design.md";
    private static final String SAMPLE_SYSTEM_REQUIREMENTS = TEST_RESOURCES_MARKDOWN
            + "/sample_system_requirements.md";

    @BeforeAll
    static void assumeSecurityManagerSupported()
    {
        TestAssumptions.assumeSecurityManagerSupported();
    }

    @Test
    void testCliExitCode_Ok()
    {
        assertExitStatusForTracedFiles(ExitStatus.OK, SAMPLE_SYSTEM_REQUIREMENTS, SAMPLE_DESIGN);
    }

    private void assertExitStatusForTracedFiles(final ExitStatus expectedStatus,
            final String... files)
    {
        assertExitStatusForCommandWithFiles(expectedStatus, TraceCommand.COMMAND_NAME, files);
    }

    private void assertExitStatusForCommandWithFiles(final ExitStatus expectedStatus,
            final String command, final String... files)
    {
        final CliArguments arguments = new CliArguments(new StandardDirectoryService());
        final List<String> values = new ArrayList<>();
        values.add(command);
        values.addAll(Arrays.asList(files));
        arguments.setUnnamedValues(values);
        assertExitWithStatus(expectedStatus.getCode(), () -> new CliStarter(arguments).run());
    }

    @Test
    void testCliExitCode_Failure()
    {
        assertExitStatusForTracedFiles(ExitStatus.FAILURE, SAMPLE_SYSTEM_REQUIREMENTS);
    }

    @Test
    void testCliExitCode_CliError()
    {
        final String[] arguments = { "--zzzzz" };
        assertExitWithStatus(ExitStatus.CLI_ERROR.getCode(), () -> CliStarter.main(arguments));
    }

    @Test
    void testExecutableJarLauncher()
    {
        JarLauncher.start(null, List.of()).waitUntilTerminated(Duration.ofSeconds(10));
    }
}
