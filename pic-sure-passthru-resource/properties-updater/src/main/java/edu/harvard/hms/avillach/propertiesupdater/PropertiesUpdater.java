package edu.harvard.hms.avillach.propertiesupdater;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.util.Iterator;
import java.util.regex.Pattern;

public class PropertiesUpdater {
    private static final String openMessage = """
    This script reads in a name, remote, common, base, and token.
    It looks for a passthru site with that name. If it can find one, it updates
    that index. Otherwise it creates a new passthru site.
    """;
    private static final Pattern siteRegex = Pattern.compile("^(passthru.remote-resource.sites\\[)(\\d+)(]\\.name)$");

    public static void main(String[] args) throws ConfigurationException {
        System.exit(run(args));
    }

    private static int run(String[] args) throws ConfigurationException {
        if (args.length != 5) {
            System.out.println("Expected 5 args: name, remote, common, base, token. Got " + args.length);
            return 1;
        }
        String name = args[0];
        String remote = args[1];
        String common = args[2];
        String base = args[3];
        String token = args[4];

        System.out.println("Updating application.properties with the following: ");
        System.out.println("\tname: " + name);
        System.out.println("\tremote: " + remote);
        System.out.println("\tcommon: " + common);
        System.out.println("\tbase: " + base);
        System.out.println("\ttoken: " + token);


        FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
            new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters().fileBased().setFileName("/application.properties"));
        PropertiesConfiguration propertiesConfiguration = builder.getConfiguration();
        propertiesConfiguration.getLayout().setGlobalSeparator("=");
        int index = getIndex(name, propertiesConfiguration);

        String prefix = "passthru.remote-resource.sites[" + index + "].";
        propertiesConfiguration.setProperty(prefix + "name", name);
        propertiesConfiguration.setProperty(prefix + "remote", remote);
        propertiesConfiguration.setProperty(prefix + "common", common);
        propertiesConfiguration.setProperty(prefix + "base", base);
        propertiesConfiguration.setProperty(prefix + "token", token);

        builder.save();
        System.out.println("Done.");
        return 0;
    }

    static int getIndex(String site, PropertiesConfiguration propertiesConfiguration) {
        int index = 0;
        Iterator<String> keys = propertiesConfiguration.getKeys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (siteRegex.matcher(key).find()) {
                if (propertiesConfiguration.getProperty(key).equals(site)) {
                    return index;
                } else {
                    index++;
                }
            }
        }
        return index;
    }
}