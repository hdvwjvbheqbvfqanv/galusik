#!/bin/sh
# Нужны JDK 25 и Gradle 9.3+
gradle build && echo "Готовый мод: build/libs/babushka-1.0.0.jar"
