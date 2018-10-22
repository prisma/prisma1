#!/usr/bin/env bash

native-image --enable-all-security-services --rerun-class-initialization-at-runtime=javax.net.ssl.SSLContext -H:IncludeResources=reference\.conf,version\.conf -H:IncludeResources=public_suffix_trie\\.json -H:IncludeResources=org/joda/time/tz/data/.* -H:ReflectionConfigurationFiles=akka_reflection_config.json -H:+JNI --verbose -cp "$CLASSPATHS""$currentlyTesting" -H:Class=Main
