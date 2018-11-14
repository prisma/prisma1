package com.prisma.natively;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

import java.io.File;

public class StaticAkkaConfig {

    public static File confFile = new File("application_conf_overrides.conf");

    public static Config reference = ConfigFactory.load();

//    public static Config overrides = ConfigFactory.parseFile(confFile);

    public static Config overrides = ConfigFactory.parseString("akka { library-extensions = [], ssl-config { default = true } }");

    public static Config config = overrides.withFallback(reference).resolve();

    static {
        System.out.println("loaded the config:");
        System.out.println(overrides.root().render(ConfigRenderOptions.concise()));
        System.out.println(config.getStringList("akka.library-extensions"));
    }
}
