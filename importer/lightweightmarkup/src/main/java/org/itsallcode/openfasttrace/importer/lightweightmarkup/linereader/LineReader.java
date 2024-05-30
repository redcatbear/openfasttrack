package org.itsallcode.openfasttrace.importer.lightweightmarkup.linereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.itsallcode.openfasttrace.api.importer.ImporterException;
import org.itsallcode.openfasttrace.api.importer.input.InputFile;

public class LineReader
{
    private static final Logger LOG = Logger.getLogger(LineReader.class.getName());

    private final InputFile file;
    private final LineReaderCallback callback;

    public LineReader(final InputFile file, final LineReaderCallback callback)
    {
        this.file = file;
        this.callback = callback;
    }

    /**
     * Start reading the file and call the callback for each line.
     */
    public void readFile()
    {
        LOG.fine(() -> "Starting import of file '" + this.file + "'");
        String previousLine = null;
        String currentLine = null;
        String nextLine = null;
        int lineNumber = 0;
        try (BufferedReader reader = this.file.createReader())
        {
            while ((nextLine = reader.readLine()) != null)
            {
                if (currentLine != null)
                {
                    callback.nextLine(new LineContext(lineNumber, previousLine, currentLine, nextLine));
                }
                ++lineNumber;
                previousLine = currentLine;
                currentLine = nextLine;
            }
            if (currentLine != null)
            {
                callback.nextLine(new LineContext(lineNumber, previousLine, currentLine, nextLine));
            }
        }
        catch (final IOException exception)
        {
            throw new ImporterException(
                    "Error reading '" + this.file.getPath() + "' at line " + lineNumber + ": "
                            + exception.getMessage(),
                    exception);

        }
        callback.finishReading();
    }
}
