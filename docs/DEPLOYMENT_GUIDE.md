# 🚀 Guía de Despliegue - Political Referrals WA

Esta guía te llevará paso a paso a través del proceso de configuración e implementación del pipeline de CI/CD para el proyecto Political Referrals WA.

## 📋 Tabla de Contenidos

1. [Prerrequisitos](#prerrequisitos)
2. [Configuración Inicial](#configuración-inicial)
3. [Configuración de GitHub Secrets](#configuración-de-github-secrets)
4. [Configuración de Google Cloud](#configuración-de-google-cloud)
5. [Pipeline de CI/CD](#pipeline-de-cicd)
6. [Despliegue Manual](#despliegue-manual)
7. [Monitoreo y Troubleshooting](#monitoreo-y-troubleshooting)
8. [Rollback](#rollback)

## 🔧 Prerrequisitos

### Software Requerido
- **Java 21+** - [Descargar aquí](https://adoptium.net/)
- **Maven 3.6+** - [Descargar aquí](https://maven.apache.org/download.cgi)
- **Docker** - [Descargar aquí](https://docs.docker.com/get-docker/)
- **Google Cloud CLI** - [Descargar aquí](https://cloud.google.com/sdk/docs/install)

### Cuentas y Servicios
- **GitHub** - Para el repositorio y GitHub Actions
- **Google Cloud Platform** - Para Cloud Run y Secret Manager
- **SonarQube** (opcional) - Para análisis de calidad de código

## ⚙️ Configuración Inicial

### 1. Clonar el Repositorio
```bash
git clone <tu-repositorio>
cd political_referrals_wa_clean
```

### 2. Verificar Dependencias
```bash
# Verificar Java
java -version  # Debe ser Java 21+

# Verificar Maven
mvn -version   # Debe ser Maven 3.6+

# Verificar Docker
docker --version
```

### 3. Configurar Variables de Entorno
```bash
# Copiar archivo de ejemplo
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Editar con tus credenciales
nano src/main/resources/application.properties
```

## 🔐 Configuración de GitHub Secrets

### 1. Ir a GitHub Repository Settings
- Navega a tu repositorio en GitHub
- Ve a **Settings** → **Secrets and variables** → **Actions**

### 2. Agregar los Siguientes Secrets

#### **GCP Configuration**
```
GCP_PROJECT_ID=tu-proyecto-id
GCP_SA_KEY=contenido-del-archivo-json-de-service-account
```

#### **SonarQube (Opcional)**
```
SONAR_TOKEN=tu-token-de-sonarqube
SONAR_HOST_URL=https://sonarcloud.io
```

#### **Notificaciones (Opcional)**
```
SLACK_WEBHOOK_URL=tu-webhook-de-slack
```

### 3. Obtener Service Account Key
```bash
# En Google Cloud Console
gcloud iam service-accounts create political-referrals-wa-sa \
    --description="Service account for Political Referrals WA" \
    --display-name="Political Referrals WA SA"

# Asignar roles necesarios
gcloud projects add-iam-policy-binding tu-proyecto-id \
    --member="serviceAccount:political-referrals-wa-sa@tu-proyecto-id.iam.gserviceaccount.com" \
    --role="roles/run.admin"

gcloud projects add-iam-policy-binding tu-proyecto-id \
    --member="serviceAccount:political-referrals-wa-sa@tu-proyecto-id.iam.gserviceaccount.com" \
    --role="roles/storage.admin"

gcloud projects add-iam-policy-binding tu-proyecto-id \
    --member="serviceAccount:political-referrals-wa-sa@tu-proyecto-id.iam.gserviceaccount.com" \
    --role="roles/secretmanager.admin"

# Crear y descargar la key
gcloud iam service-accounts keys create political-referrals-wa-sa.json \
    --iam-account=political-referrals-wa-sa@tu-proyecto-id.iam.gserviceaccount.com

# Copiar el contenido del archivo JSON al secret GCP_SA_KEY
cat political-referrals-wa-sa.json
```

## ☁️ Configuración de Google Cloud

### 1. Autenticación
```bash
gcloud auth login
gcloud config set project tu-proyecto-id
```

### 2. Habilitar APIs Necesarias
```bash
gcloud services enable \
    run.googleapis.com \
    secretmanager.googleapis.com \
    containerregistry.googleapis.com \
    cloudbuild.googleapis.com
```

### 3. Configurar Secrets Automáticamente
```bash
# Hacer ejecutable el script
chmod +x scripts/setup-gcp-secrets.sh

# Ejecutar configuración
./scripts/setup-gcp-secrets.sh
```

### 4. Configurar Firestore
```bash
# Crear base de datos Firestore
gcloud firestore databases create \
    --region=us-central1 \
    --type=firestore-native
```

## 🔄 Pipeline de CI/CD

### Estructura del Pipeline

El proyecto tiene **DOS workflows separados** para diferentes propósitos:

#### 🚀 **Workflow de Producción** (`ci-cd.yml`)
- **Trigger**: Solo push a `main`
- **Acciones**: Build, test, análisis, Docker, **despliegue automático a producción**
- **Objetivo**: Despliegue automático a Cloud Run en producción

#### 🧪 **Workflow de Desarrollo** (`ci-dev.yml`)
- **Trigger**: Push a `develop` y Pull Requests
- **Acciones**: Build, test, análisis
- **Objetivo**: Validación de código sin despliegue

### Flujo de Trabajo Recomendado

```
develop → Pull Request → main → Despliegue Automático a Producción
   ↓           ↓         ↓              ↓
   Build    Code      Merge        Deploy to
   & Test   Review    to main      Production
```

### Jobs del Pipeline

#### 1. **Build and Test**
- Compila el proyecto con Maven
- Ejecuta tests unitarios
- Genera el archivo JAR
- Sube artifacts para uso posterior

#### 2. **Code Quality Analysis**
- Análisis con SonarQube
- Escaneo de seguridad con OWASP
- Verificación de dependencias

#### 3. **Docker Build**
- Construye imagen Docker
- Sube a Google Container Registry
- Aplica cache para optimización

#### 4. **Deploy to Cloud Run**
- Despliega a Google Cloud Run
- Configura variables de entorno
- Realiza health check
- Configura autoscaling

#### 5. **Notifications**
- Notifica estado del despliegue
- Envía notificaciones a Slack (opcional)

### Monitoreo del Pipeline

1. Ve a **Actions** en tu repositorio de GitHub
2. Selecciona el workflow **CI/CD Pipeline**
3. Monitorea la ejecución en tiempo real
4. Revisa logs en caso de fallos

## 🚀 Despliegue Manual

### 1. Despliegue Local
```bash
# Hacer ejecutable el script
chmod +x scripts/deploy-local.sh

# Ejecutar despliegue local
./scripts/deploy-local.sh

# O especificar entorno
./scripts/deploy-local.sh dev
```

### 2. Despliegue a Cloud Run
```bash
# Usar configuración generada
gcloud run services replace deploy/cloud-run-configured.yaml

# O despliegue directo
gcloud run deploy political-referrals-wa \
    --image gcr.io/tu-proyecto-id/political-referrals-wa:latest \
    --region us-central1 \
    --platform managed \
    --allow-unauthenticated \
    --port 8080 \
    --memory 2Gi \
    --cpu 2
```

### 3. Verificar Despliegue
```bash
# Obtener URL del servicio
gcloud run services describe political-referrals-wa \
    --region=us-central1 \
    --format='value(status.url)'

# Health check
curl https://tu-servicio-url/actuator/health

# Ver logs
gcloud logs read --service=political-referrals-wa --limit=50
```

## 📊 Monitoreo y Troubleshooting

### 1. Logs de Cloud Run
```bash
# Ver logs en tiempo real
gcloud logs tail --service=political-referrals-wa

# Filtrar por nivel
gcloud logs read --service=political-referrals-wa --filter="severity>=ERROR"
```

### 2. Métricas de la Aplicación
```bash
# Endpoint de métricas
curl https://tu-servicio-url/api/metrics/gemini

# Health check detallado
curl https://tu-servicio-url/actuator/health
```

### 3. Problemas Comunes

#### **Error de Autenticación**
```bash
# Verificar service account
gcloud auth list
gcloud config get-value project

# Reautenticar si es necesario
gcloud auth application-default login
```

#### **Error de Permisos**
```bash
# Verificar roles del service account
gcloud projects get-iam-policy tu-proyecto-id \
    --flatten="bindings[].members" \
    --filter="bindings.members:political-referrals-wa-sa@tu-proyecto-id.iam.gserviceaccount.com"
```

#### **Error de Build**
```bash
# Limpiar y recompilar
mvn clean package -DskipTests

# Verificar versión de Java
java -version
mvn -version
```

## 🔄 Rollback

### 1. Rollback Automático
El pipeline incluye health checks que pueden detectar fallos automáticamente.

### 2. Rollback Manual
```bash
# Listar versiones disponibles
gcloud run revisions list --service=political-referrals-wa --region=us-central1

# Hacer rollback a versión anterior
gcloud run services update-traffic political-referrals-wa \
    --to-revisions=REVISION_NAME=100 \
    --region=us-central1
```

### 3. Rollback de Docker Image
```bash
# Desplegar versión anterior
gcloud run deploy political-referrals-wa \
    --image gcr.io/tu-proyecto-id/political-referrals-wa:COMMIT_ANTERIOR \
    --region us-central1
```

## 🧪 Testing del Pipeline

### 1. Test Local
```bash
# Ejecutar tests unitarios
mvn test

# Ejecutar tests de integración
mvn verify

# Test de seguridad
mvn dependency:check
```

### 2. Test de Docker
```bash
# Construir imagen localmente
docker build -t political-referrals-wa:test .

# Ejecutar contenedor
docker run -p 8080:8080 political-referrals-wa:test

# Health check
curl http://localhost:8080/actuator/health
```

### 3. Test de Despliegue
```bash
# Desplegar a staging (si tienes)
gcloud run deploy political-referrals-wa-staging \
    --image gcr.io/tu-proyecto-id/political-referrals-wa:latest \
    --region us-central1
```

## 📈 Optimizaciones

### 1. Cache de Maven
El pipeline ya incluye cache de dependencias de Maven para builds más rápidos.

### 2. Cache de Docker
Se utiliza GitHub Actions cache para optimizar builds de Docker.

### 3. Parámetros de Cloud Run
```yaml
# En cloud-run.yaml
spec:
  template:
    spec:
      containerConcurrency: 80
      timeoutSeconds: 300
      containers:
      - resources:
          limits:
            cpu: "2"
            memory: 2Gi
        livenessProbe:
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          initialDelaySeconds: 5
          periodSeconds: 5
```

## 🔒 Seguridad

### 1. Secrets Management
- Todos los secrets están en Google Secret Manager
- No se exponen en el código
- Rotación automática de credenciales

### 2. Network Security
- Cloud Run con HTTPS obligatorio
- Firewall configurado
- VPC (opcional) para aislamiento

### 3. IAM
- Principio de menor privilegio
- Service account dedicado
- Roles específicos por funcionalidad

## 📞 Soporte

### Enlaces Útiles
- [Google Cloud Run Documentation](https://cloud.google.com/run/docs)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

### Comandos de Diagnóstico
```bash
# Estado del servicio
gcloud run services describe political-referrals-wa --region=us-central1

# Logs recientes
gcloud logs read --service=political-referrals-wa --limit=100

# Métricas del servicio
gcloud run services list --region=us-central1
```

---

## 🎯 Resumen de Pasos

1. ✅ **Configurar GitHub Secrets**
2. ✅ **Configurar Google Cloud**
3. ✅ **Ejecutar setup-gcp-secrets.sh**
4. ✅ **Configurar flujo de trabajo:**
   - Trabajar en rama `develop`
   - Crear Pull Request a `main`
   - Hacer merge a `main` → **Despliegue automático a producción**
5. ✅ **Monitorear pipeline en GitHub Actions**
6. ✅ **Verificar despliegue en Cloud Run**

## 🔄 Flujo de Trabajo Diario

### Para Desarrollo
```bash
# Crear rama de desarrollo
git checkout -b develop
git push origin develop

# Trabajar en features
git add .
git commit -m "feat: nueva funcionalidad"
git push origin develop
```

### Para Producción
```bash
# Crear Pull Request desde develop a main
# Una vez aprobado y mergeado, se despliega automáticamente

# O si necesitas deploy manual (solo para emergencias)
git checkout main
git pull origin main
# El workflow se ejecutará automáticamente
```

¡Tu pipeline de CI/CD está listo! 🚀 