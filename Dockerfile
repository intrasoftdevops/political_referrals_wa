# Usa una imagen base de Java 21
FROM openjdk:21-jdk-slim

# Establece el directorio de trabajo
WORKDIR /app

# Copia el archivo jar construido
COPY target/political_referrals_wa-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto dinámico (Cloud Run proporciona PORT)
EXPOSE 8080

# Comando para ejecutar la aplicación con perfil de producción y optimizaciones para Cloud Run
ENTRYPOINT ["java", \
  "-Dserver.port=${PORT:-8080}", \
  "-Dspring.profiles.active=prod", \
  "-Xmx1g", \
  "-Xms512m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", \
  "app.jar"] 