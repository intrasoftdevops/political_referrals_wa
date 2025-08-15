# Usa una imagen base de Java 21
FROM openjdk:21-jdk-slim

# Establece el directorio de trabajo
WORKDIR /app

# Copia el archivo jar construido
COPY target/political_referrals_wa-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto 8080 (Cloud Run estándar)
EXPOSE 8080

# Configura las variables de entorno para producción
# No necesitamos SPRING_PROFILES_ACTIVE porque usamos application.properties por defecto

# Comando para ejecutar la aplicación con puerto explícito
ENTRYPOINT ["java", "-Dserver.port=8080", "-jar", "app.jar"] 