package org.itsallcode.openfasttrace.cli;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.exasol.mavenprojectversiongetter.MavenProjectVersionGetter;

class JarLauncher
{
    private static final Logger LOG = Logger.getLogger(ProcessOutputConsumer.class.getName());

    private final Process process;
    private final ProcessOutputConsumer consumer;

    private JarLauncher(final Process process, final ProcessOutputConsumer consumer)
    {
        this.process = process;
        this.consumer = consumer;
    }

    public static JarLauncher start(final Path workingDir, final List<String> args)
    {
        final Path jarPath = getExecutableJar();
        if (!Files.exists(jarPath))
        {
            throw new IllegalStateException(
                    "Executable JAR not found at %s. Run 'mvn -T1C package -DskipTests' to build it.");
        }
        final List<String> command = new ArrayList<>();
        command.addAll(asList(getJavaExecutable().toString(), "-jar", getExecutableJar().toString()));
        command.addAll(args);
        final ProcessBuilder processBuilder = new ProcessBuilder(command.toArray(String[]::new))
                .redirectErrorStream(false);
        if (workingDir != null)
        {
            processBuilder.directory(workingDir.toFile());
        }
        try
        {
            final Process process = processBuilder.start();
            final ProcessOutputConsumer consumer = new ProcessOutputConsumer(process, Duration.ofSeconds(1));
            consumer.start();
            return new JarLauncher(process, consumer);
        }
        catch (final IOException exception)
        {
            throw new UncheckedIOException("Failed to start process %s in working dir %s: %s".formatted(command,
                    workingDir, exception.getMessage()), exception);
        }
    }

    private static Path getExecutableJar()
    {
        return Path.of("target")
                .resolve("openfasttrace-%s.jar".formatted(getCurrentProjectVersion()))
                .toAbsolutePath();
    }

    private static String getCurrentProjectVersion()
    {
        return MavenProjectVersionGetter.getProjectRevision(Path.of("../parent/pom.xml").toAbsolutePath());
    }

    private static Path getJavaExecutable()
    {
        return ProcessHandle.current().info().command()
                .map(Path::of)
                .orElseThrow(() -> new IllegalStateException("Java executable not found"));
    }

    void waitUntilTerminated(final Duration timeout)
    {
        waitForProcessTerminated(timeout);
        LOG.fine("Process %d terminated with exit code %d".formatted(process.pid(), exitValue()));
        consumer.waitForStreamsClosed();
    }

    private void waitForProcessTerminated(final Duration timeout)
    {
        try
        {
            LOG.finest("Waiting %s for process %d to terminate...".formatted(timeout, process.pid()));
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS))
            {
                throw new IllegalStateException(
                        "Timeout while waiting %s for process %d".formatted(timeout, process.pid()));
            }
        }
        catch (final InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting %s for process to finish".formatted(timeout),
                    exception);
        }
    }

    int exitValue()
    {
        return process.exitValue();
    }
}
