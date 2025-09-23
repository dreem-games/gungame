# Настройка проекта

## Настройка java в intellij idea
Для запуска проекта требуется java 21.
Порядок её настройки:
1) Настройка jdk проекта  
    1.1) ctrl+alt+shift+S (или File -> Project Structure)  
    1.2.1) Project -> Project SDK -> выбрать имеющуюся jdk 21  
    1.2.2) если jdk 21 не установлено, то Add SDK -> Download JDK -> 21
2) Настройка jdk gradle
    2.1) ctrl+alt+S File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JVM 
    2.2) В пункте Gradle JVM Выбрать имеющуюся jdk 21
3) Установите плагин Lombok для IntelliJ Idea, если не установлен, для корректной работы аннотаций.