@echo off
rem Нужны установленные JDK 25 и Gradle 9.3+ (или откройте проект в IntelliJ IDEA)
gradle build
echo.
echo Готовый мод: build\libs\babushka-1.0.0.jar
pause
