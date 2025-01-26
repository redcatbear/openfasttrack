package org.itsallcode.openfasttrace.cli;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class ProcessOutputConsumer
{
    private static final Logger LOG = Logger.getLogger(ProcessOutputConsumer.class.getName());
    private final Executor executor;
    private final Process process;
    private final ProcessStreamConsumer stdOutConsumer;
    private final ProcessStreamConsumer stdErrConsumer;

    ProcessOutputConsumer(final Process process, final Duration streamCloseTimeout)
    {
        this(createThreadExecutor(), process, new ProcessStreamConsumer("stdout", streamCloseTimeout),
                new ProcessStreamConsumer("stderr", streamCloseTimeout));
    }

    ProcessOutputConsumer(final Executor executor, final Process process,
            final ProcessStreamConsumer stdOutConsumer, final ProcessStreamConsumer stdErrConsumer)
    {
        this.executor = executor;
        this.process = process;
        this.stdOutConsumer = stdOutConsumer;
        this.stdErrConsumer = stdErrConsumer;
    }

    private static Executor createThreadExecutor()
    {
        return runnable -> {
            final Thread thread = new Thread(runnable);
            thread.setName(ProcessOutputConsumer.class.getSimpleName());
            thread.setUncaughtExceptionHandler(new LoggingExceptionHandler());
            thread.start();
        };
    }

    void start()
    {
        LOG.finest("Start reading stdout and stderr streams in background...");
        executor.execute(() -> {
            readStream(process.getInputStream(), stdOutConsumer);
        });
        executor.execute(() -> {
            readStream(process.getErrorStream(), stdErrConsumer);
        });
    }

    private void readStream(final InputStream stream, final ProcessStreamConsumer consumer)
    {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
        {
            String line = null;
            LOG.finest("Start reading from '%s' stream...".formatted(consumer.name));
            while ((line = reader.readLine()) != null)
            {
                consumer.accept(line);
            }
            LOG.finest("Stream '%s' finished".formatted(consumer.name));
            consumer.streamFinished();
        }
        catch (final IOException exception)
        {
            LOG.log(Level.WARNING, "Reading input stream failed: %s".formatted(exception.getMessage()), exception);
            consumer.streamFinished();
        }
    }

    String getStdOut()
    {
        return stdOutConsumer.getContent();
    }

    String getStdErr()
    {
        return stdErrConsumer.getContent();
    }

    void waitForStreamsClosed()
    {
        stdOutConsumer.waitUntilStreamClosed();
        stdErrConsumer.waitUntilStreamClosed();
    }

    private static class ProcessStreamConsumer
    {
        private final CountDownLatch streamFinished = new CountDownLatch(1);
        private final StringBuilder builder = new StringBuilder();
        private final String name;
        private final Duration streamCloseTimeout;

        ProcessStreamConsumer(final String name, final Duration streamCloseTimeout)
        {
            this.name = name;
            this.streamCloseTimeout = streamCloseTimeout;
        }

        String getContent()
        {
            return builder.toString();
        }

        void streamFinished()
        {
            streamFinished.countDown();
        }

        void accept(final String line)
        {
            LOG.fine("%s > %s".formatted(name, line));
            builder.append(line).append("\n");
        }

        void waitUntilStreamClosed()
        {
            LOG.finest("Waiting %s for stream '%s' to close".formatted(streamCloseTimeout, name));
            if (!await(streamCloseTimeout))
            {
                throw new IllegalStateException(
                        "Stream '%s' not closed within timeout of %s".formatted(name, streamCloseTimeout));
            }
            else
            {
                LOG.finest("Stream '%s' closed".formatted(name));
            }
        }

        private boolean await(final Duration timeout)
        {
            try
            {
                return streamFinished.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException exception)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for stream '%s' to be closed: %s"
                        .formatted(name, exception.getMessage()), exception);
            }
        }
    }

    private static class LoggingExceptionHandler implements UncaughtExceptionHandler
    {
        @Override
        public void uncaughtException(final Thread thread, final Throwable exception)
        {
            LOG.log(Level.WARNING,
                    "Exception occurred in thread '%s': %s".formatted(thread.getName(), exception.toString()),
                    exception);
        }
    }
}
