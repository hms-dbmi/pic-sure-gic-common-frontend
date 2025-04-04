package edu.harvard.hms.avillach.propertiesupdater;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesUpdaterTest {

    @Test
    void shouldFindSiteThatExists(@TempDir File tmp) throws IOException, ConfigurationException {
        Path p = Path.of(tmp.getAbsolutePath(), "/application.properties");
        String s = """
            passthru.remote-resource.sites[0].name=foo
            passthru.remote-resource.sites[0].y=1
            passthru.remote-resource.sites[1].name=bar
            passthru.remote-resource.sites[1].y=2
            passthru.remote-resource.sites[2].name=baz
            passthru.remote-resource.sites[2].y=3
            """;
        Files.writeString(p, s);

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
            new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters().fileBased()
                .setFileName(p.toString()));
        PropertiesConfiguration propertiesConfiguration = builder.getConfiguration();

        int bar = new PropertiesUpdater().getIndex("bar", propertiesConfiguration);
        assertEquals(1, bar);
    }

    @Test
    void shouldFindSiteThatDoesNotExists(@TempDir File tmp) throws IOException, ConfigurationException {
        Path p = Path.of(tmp.getAbsolutePath(), "/application.properties");
        String s = """
            passthru.remote-resource.sites[0].name=foo
            passthru.remote-resource.sites[0].y=1
            passthru.remote-resource.sites[1].name=bar
            passthru.remote-resource.sites[1].y=2
            passthru.remote-resource.sites[2].name=baz
            passthru.remote-resource.sites[2].y=3
            """;
        Files.writeString(p, s);

        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
            new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters().fileBased()
                    .setFileName(p.toString()));
        PropertiesConfiguration propertiesConfiguration = builder.getConfiguration();

        int bar = PropertiesUpdater.getIndex("frank", propertiesConfiguration);
        assertEquals(3, bar);
    }
}