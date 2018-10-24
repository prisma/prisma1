package com.prisma.native

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions

object StaticAkkaConfig {
//  var confFile          = new File("application_conf_overrides.conf")
  //    public static Config overrides = ConfigFactory.parseFile(confFile);
  var reference: Config = ConfigFactory.load
  var overrides: Config = ConfigFactory.parseString("akka { library-extensions = [], ssl-config { default = true } }")
  var config: Config    = overrides.withFallback(reference).resolve

  System.out.println(overrides.root.render(ConfigRenderOptions.concise))
  System.out.println(config.getStringList("akka.library-extensions"))
}
