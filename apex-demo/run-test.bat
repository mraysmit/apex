@echo off
cd /d "%~dp0"

set CLASSPATH=target\test-classes;..\apex-core\target\classes
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\org\yaml\snakeyaml\2.2\snakeyaml-2.2.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\2.0.13\slf4j-api-2.0.13.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\org\springframework\spring-expression\6.1.10\spring-expression-6.1.10.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\org\springframework\spring-core\6.1.10\spring-core-6.1.10.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\org\springframework\spring-jcl\6.1.10\spring-jcl-6.1.10.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.17.1\jackson-core-2.17.1.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.17.1\jackson-databind-2.17.1.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.17.1\jackson-annotations-2.17.1.jar
set CLASSPATH=%CLASSPATH%;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.17.1\jackson-dataformat-yaml-2.17.1.jar

echo Running Cross-File Rule Group Reference Test...
java dev.mars.apex.demo.rulegroups.SimpleCrossFileTest
