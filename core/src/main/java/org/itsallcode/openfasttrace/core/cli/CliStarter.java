package org.itsallcode.openfasttrace.core.cli;

import java.util.Optional;

import org.itsallcode.openfasttrace.api.cli.DirectoryService;
import org.itsallcode.openfasttrace.core.cli.commands.*;
import org.itsallcode.openfasttrace.core.cli.logging.LoggingConfigurator;

/**
 * The main entry point class for the command line application.
 */
public class CliStarter
{
    private final CliArguments arguments;

    /**
     * Create a new cli starter.
     * 
     * @param arguments
     *            the arguments for the starter.
     */
    public CliStarter(final CliArguments arguments)
    {
        this.arguments = arguments;
    }

    /**
     * Main entry point for the command line application
     * 
     * @param args
     *            command line parameters
     */
    public static void main(final String[] args)
    {
        final DirectoryService directoryService = new StandardDirectoryService();
        main(args, directoryService);
    }

    /**
     * Auxiliary entry point to the command line application that allows
     * injection of a {@link DirectoryService}.
     * 
     * @param args
     *            command line arguments.
     * @param directoryService
     *            directory service for getting the current directory. This
     *            allows injecting a mock in unit tests.
     */
    public static void main(final String[] args, final DirectoryService directoryService)
    {
        final CliArguments arguments = parseCommandLineArguments(args, directoryService);
        final ArgumentValidator validator = new ArgumentValidator(arguments);
        if (validator.isValid())
        {
            LoggingConfigurator.create(arguments).configureLogging();
            new CliStarter(arguments).run();
        }
        else
        {
            printToStdError(
                    "oft: " + validator.getError() + "\n" + validator.getSuggestion() + "\n");
            exit(ExitStatus.CLI_ERROR);
        }
    }

    private static CliArguments parseCommandLineArguments(final String[] args,
            final DirectoryService directoryService)
    {
        final CliArguments arguments = new CliArguments(directoryService);
        try
        {
            new CommandLineInterpreter(args, arguments).parse();
        }
        catch (final CliException e)
        {
            printToStdError("oft: " + e.getMessage());
            exit(ExitStatus.CLI_ERROR);
        }
        return arguments;
    }

    // Writing to standard error by intention
    @SuppressWarnings("squid:S106")
    private static void printToStdError(final String message)
    {
        System.err.println(message);
    }

    /**
     * Process the command line arguments and execute the commands.
     */
    // [impl->dsn~cli.command-selection~1]
    public void run()
    {
        final Optional<String> command = this.arguments.getCommand();
        if (!command.isPresent())
        {
            new HelpCommand().run();
            throw new IllegalStateException("Command missing trying to execute OFT mode.");
        }
        final Performable performable;
        switch (command.get())
        {
        case ConvertCommand.COMMAND_NAME:
            performable = new ConvertCommand(this.arguments);
            break;
        case TraceCommand.COMMAND_NAME:
            performable = new TraceCommand(this.arguments);
            break;
        case HelpCommand.COMMAND_NAME:
            performable = new HelpCommand();
            break;
        default:
            new HelpCommand().run();
            exit(ExitStatus.CLI_ERROR);
            return;
        }
        if (performable.run())
        {
            exit(ExitStatus.OK);
        }
        else
        {
            exit(ExitStatus.FAILURE);
        }
    }

    // [impl->dsn~cli.tracing.exit-status~1]
    private static void exit(final ExitStatus exitStatus)
    {
        System.exit(exitStatus.getCode());
    }
}
