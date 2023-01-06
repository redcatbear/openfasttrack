package org.itsallcode.openfasttrace.testutil.importer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.itsallcode.openfasttrace.api.importer.*;
import org.itsallcode.openfasttrace.api.importer.input.RealFileInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for {@link ImporterFactory} tests.
 *
 * @param <T>
 *            type of the factory under test
 */
@ExtendWith(MockitoExtension.class)
public abstract class ImporterFactoryTestBase<T extends ImporterFactory>
{
    @Mock
    protected ImporterContext contextMock;

    @BeforeEach
    public void initMocks()
    {
        lenient().when(this.contextMock.getImportSettings()).thenReturn(ImportSettings.createDefault());
    }

    @Test
    void testSupportedFileNames()
    {
        assertSupported(getSupportedFilenames(), true);
    }

    @Test
    void testUnsupportedFileNames()
    {
        assertSupported(getUnsupportedFilenames(), false);
    }

    @Test
    void testCreateImporterThrowsExceptionForMissingFile()
    {
        final Path supportedPath = Paths.get("dir", getSupportedFilenames().get(0));
        assertThrows(ImporterException.class, //
                () -> createAndInitialize()
                        .createImporter(RealFileInput.forPath(supportedPath), null).runImport(), //
                "Error reading \"" + supportedPath + "\"");
    }

    private T createAndInitialize()
    {
        final T factory = createFactory();
        factory.init(this.contextMock);
        return factory;
    }

    @Test
    void testInit()
    {
        final T factory = createFactory();
        factory.init(this.contextMock);
        assertThat(factory.getContext(), sameInstance(this.contextMock));
    }

    @Test
    void testMissingContextThrowsException()
    {
        final T factory = createFactory();
        factory.init(null);
        assertThrows(NullPointerException.class, factory::getContext,
                "Context was not initialized");
    }

    private void assertSupported(final List<String> filenames, final boolean expectedResult)
    {
        final T factory = createFactory();
        for (final String filename : filenames)
        {
            final Path path = Paths.get("dir", filename);
            assertThat(path.toString(), factory.supportsFile(RealFileInput.forPath(path)),
                    equalTo(expectedResult));
        }
    }

    protected abstract T createFactory();

    protected abstract List<String> getSupportedFilenames();

    protected abstract List<String> getUnsupportedFilenames();
}
