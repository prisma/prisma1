package com.prisma.native_image;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

public class StaticAkkaConfig {
    public static Config reference = ConfigFactory.load();

    public static Config overrides = ConfigFactory.parseString("akka { library-extensions = [], ssl-config { default = true } }");

    public static Config config = overrides.withFallback(reference).resolve();

    static {
        System.out.println("loaded the config:");
        System.out.println(overrides.root().render(ConfigRenderOptions.concise()));
        System.out.println(config.getStringList("akka.library-extensions"));
    }
}
