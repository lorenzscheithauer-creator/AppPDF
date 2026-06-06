#!/usr/bin/env sh
# Lightweight repository wrapper for environments where the Gradle wrapper JAR is unavailable.
exec gradle "$@"
