# Migración de Analytics a Servicio Local

## Resumen

Este documento describe la migración completa de la funcionalidad de `user-referrals-metrics` al proyecto `political_referrals_wa_clean`, eliminando la dependencia del servicio externo.

## Cambios Implementados

### 1. Nuevos Servicios Creados

#### `LocalAnalyticsService`
- **Ubicación**: `src/main/java/com/politicalreferralswa/service/LocalAnalyticsService.java`
- **Funcionalidad**: Implementa toda la lógica de cálculo de métricas localmente
- **Características**:
  - Acceso directo a Firestore
  - Caché integrado con Redis
  - Consultas paralelas para mejor rendimiento
  - Manejo de errores robusto

#### `LocalAuditService`
- **Ubicación**: `src/main/java/com/politicalreferralswa/service/LocalAuditService.java`
- **Funcionalidad**: Reemplaza el sistema de auditoría del servicio externo
- **Características**:
  - Registro de accesos a analytics
  - Registro de errores
  - Auditoría de autenticación
  - Almacenamiento asíncrono en Firestore

### 2. Nuevo Controlador

#### `AnalyticsController`
- **Ubicación**: `src/main/java/com/politicalreferralswa/controllers/AnalyticsController.java`
- **Endpoints**:
  - `GET /api/v1/analytics/user-stats` - Estadísticas del usuario
  - `GET /api/v1/analytics/health` - Health check
  - `DELETE /api/v1/analytics/cache/user/{userId}` - Limpiar caché de usuario
  - `DELETE /api/v1/analytics/cache/clear` - Limpiar todo el caché
  - `GET /api/v1/analytics/test` - Endpoint de prueba

### 3. Configuración de Caché

#### `CacheConfig`
- **Ubicación**: `src/main/java/com/politicalreferralswa/config/CacheConfig.java`
- **Funcionalidad**: Configuración de Redis para caché
- **Características**:
  - TTL configurable por tipo de caché
  - Serialización JSON automática
  - Configuración flexible de conexión

## Configuración Requerida

### Variables de Entorno

```properties
# Redis Configuration
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=
SPRING_REDIS_DATABASE=0

# Firestore (ya configurado)
SPRING_CLOUD_GCP_PROJECT_ID=tu-proyecto-id
```

### Dependencias Maven

```xml
<!-- Redis para Caché -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Jackson para Serialización -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## Estructura de Datos en Firestore

### Colecciones Utilizadas

1. **`users`** - Perfiles de usuario
2. **`rankings`** - Estadísticas de ranking
3. **`userGeography`** - Datos geográficos
4. **`userReferrals`** - Estadísticas de referidos
5. **`audit_logs`** - Logs de auditoría

### Formato de Datos

#### Rankings
```json
{
  "userId": "573227281752",
  "todayPosition": 5,
  "todayPoints": 150,
  "weekPosition": 12,
  "weekPoints": 450,
  "monthPosition": 25,
  "monthPoints": 1200,
  "timestamp": "2025-01-27T10:00:00Z"
}
```

#### Geografía
```json
{
  "regionPosition": 3,
  "regionTotalParticipants": 150,
  "regionPercentile": 98.0,
  "cityPosition": 7,
  "cityTotalParticipants": 45,
  "cityPercentile": 84.4
}
```

## Uso del Servicio

### 1. Obtener Estadísticas de Usuario

```java
@Autowired
private LocalAnalyticsService localAnalyticsService;

public void ejemplo() {
    UserStatsResponse stats = localAnalyticsService.getUserStats("573227281752");
    
    // Acceder a los datos
    String nombre = stats.getName();
    int posicionHoy = stats.getRanking().getToday().getPosition();
    int totalReferidos = stats.getReferrals().getTotalInvited();
}
```

### 2. Limpiar Caché

```java
// Limpiar caché de un usuario específico
localAnalyticsService.clearUserCache("573227281752");

// Limpiar todo el caché
localAnalyticsService.clearAllCache();
```

### 3. Auditoría

```java
@Autowired
private LocalAuditService localAuditService;

public void ejemploAuditoria() {
    localAuditService.logAnalyticsAccess(
        "573227281752", "session123", "192.168.1.1", 
        "Mozilla/5.0...", false, 150, "user_stats"
    );
}
```

## Endpoints REST

### Autenticación
Todos los endpoints requieren un token JWT válido en el header `Authorization`:
```
Authorization: Bearer <token-jwt>
```

### Ejemplo de Uso con curl

```bash
# Obtener estadísticas del usuario
curl -X GET "http://localhost:8080/api/v1/analytics/user-stats" \
  -H "Authorization: Bearer <token-jwt>" \
  -H "X-Session-ID: session123"

# Health check
curl -X GET "http://localhost:8080/api/v1/analytics/health"

# Limpiar caché de usuario
curl -X DELETE "http://localhost:8080/api/v1/analytics/cache/user/573227281752" \
  -H "Authorization: Bearer <token-jwt>"
```

## Ventajas de la Migración

### ✅ Beneficios
1. **Menor latencia** - No hay llamadas HTTP externas
2. **Mejor rendimiento** - Acceso directo a Firestore
3. **Menor complejidad** - Un solo proyecto para mantener
4. **Menor costo** - No necesitas desplegar dos servicios
5. **Mejor control** - Acceso completo al código y datos

### 🔄 Cambios en el Comportamiento
1. **AnalyticsService** ahora usa el servicio local por defecto
2. **Método de fallback** disponible para usar el servicio externo si es necesario
3. **Caché integrado** con TTL configurable
4. **Auditoría completa** de todas las operaciones

## Próximos Pasos

### 1. Pruebas
- [ ] Verificar que todos los endpoints funcionen correctamente
- [ ] Comparar respuestas con el servicio externo
- [ ] Validar rendimiento y latencia
- [ ] Probar el sistema de caché

### 2. Despliegue
- [ ] Configurar Redis en el entorno de producción
- [ ] Actualizar variables de entorno
- [ ] Desplegar la nueva versión
- [ ] Monitorear logs y métricas

### 3. Eliminación del Servicio Externo
- [ ] Confirmar que todo funciona correctamente
- [ ] Actualizar configuraciones para no depender del endpoint externo
- [ ] Eliminar el proyecto `user-referrals-metrics`
- [ ] Limpiar configuraciones relacionadas

## Troubleshooting

### Problemas Comunes

#### Redis Connection Failed
```
Error: Redis connection failed
Solución: Verificar que Redis esté ejecutándose y la configuración sea correcta
```

#### Firestore Permission Denied
```
Error: Permission denied accessing Firestore
Solución: Verificar las credenciales de servicio y permisos
```

#### Cache Not Working
```
Error: Caché no funciona
Solución: Verificar que @EnableCaching esté habilitado y Redis esté configurado
```

### Logs Útiles

```bash
# Ver logs de analytics
grep "LocalAnalyticsService" logs/application.log

# Ver logs de caché
grep "CacheConfig" logs/application.log

# Ver logs de auditoría
grep "LocalAuditService" logs/application.log
```

## Contacto

Para preguntas o problemas con la migración, revisar los logs de la aplicación o consultar la documentación de Spring Boot y Redis.
