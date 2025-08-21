# Control de IA del Sistema - Political Referrals WA

Este documento describe cómo usar los nuevos endpoints para controlar globalmente la IA del sistema.

## Descripción General

El sistema ahora incluye un control global que permite activar o desactivar la IA para todos los usuarios. Cuando la IA está deshabilitada, los usuarios en estado "COMPLETED" son atendidos por agentes humanos a través de WATI en lugar de recibir respuestas automáticas de la IA.

## Endpoints Disponibles

### 1. Obtener Estado de la IA
**GET** `/api/system/ai/status`

Obtiene el estado actual de la IA del sistema.

**Respuesta:**
```json
{
  "aiEnabled": true,
  "status": "HABILITADA",
  "message": "La IA está habilitada y los usuarios COMPLETED interactúan con ella"
}
```

### 2. Habilitar IA
**POST** `/api/system/ai/enable`

Habilita la IA del sistema para que todos los usuarios COMPLETED interactúen con ella.

**Respuesta:**
```json
{
  "aiEnabled": true,
  "status": "HABILITADA",
  "message": "La IA del sistema ha sido habilitada. Todos los usuarios COMPLETED ahora interactuarán con la IA."
}
```

### 3. Deshabilitar IA
**POST** `/api/system/ai/disable`

Deshabilita la IA del sistema para que los usuarios COMPLETED sean atendidos por agentes humanos.

**Respuesta:**
```json
{
  "aiEnabled": false,
  "status": "DESHABILITADA",
  "message": "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI."
}
```

### 4. Cambiar Estado de la IA
**POST** `/api/system/ai/toggle`

Cambia el estado actual de la IA (si está habilitada la deshabilita, si está deshabilitada la habilita).

**Respuesta:**
```json
{
  "aiEnabled": false,
  "status": "DESHABILITADA",
  "message": "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI."
}
```

### 5. Establecer Estado Específico
**POST** `/api/system/ai/set`

Establece un estado específico para la IA del sistema.

**Body:**
```json
{
  "enabled": false
}
```

**Respuesta:**
```json
{
  "aiEnabled": false,
  "status": "DESHABILITADA",
  "message": "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI."
}
```

## Estado Inicial

Al iniciar el sistema, la IA está **HABILITADA por defecto**. Para cambiar este comportamiento, debes usar los endpoints de configuración.

## Configuración del Sistema

El sistema no depende de variables de entorno para el control de la IA. Todo se maneja a través de los endpoints REST, lo que proporciona control total y dinámico del comportamiento del sistema.

## Comportamiento del Sistema

### Con IA Habilitada (Estado Normal)
- Los usuarios en estado "COMPLETED" interactúan directamente con la IA
- Se procesan consultas de analytics, solicitudes de tribu y preguntas generales
- Respuestas automáticas e instantáneas

### Con IA Deshabilitada
- Los usuarios en estado "COMPLETED" reciben un mensaje informativo
- El mensaje indica que serán atendidos por un agente humano
- Los agentes humanos pueden ver los mensajes en WATI y responder manualmente

## Comandos Curl para Pruebas

### **Prueba Completa del Sistema de Control de IA**

#### **Paso 1: Verificar Estado Inicial**
```bash
curl -X GET http://localhost:8080/api/system/ai/status
```

#### **Paso 2: Deshabilitar la IA (Apagado)**
```bash
curl -X POST http://localhost:8080/api/system/ai/disable \
  -H "X-API-Key: dev-secure-api-key-2024"
```

#### **Paso 3: Verificar que se Deshabilitó**
```bash
curl -X GET http://localhost:8080/api/system/ai/status
```

#### **Paso 4: Habilitar la IA (Encendido)**
```bash
curl -X POST http://localhost:8080/api/system/ai/enable \
  -H "X-API-Key: dev-secure-api-key-2024"
```

#### **Paso 5: Verificar Estado Final**
```bash
curl -X GET http://localhost:8080/api/system/ai/status
```

#### **Comandos Adicionales**

**Cambio Rápido (Toggle):**
```bash
curl -X POST http://localhost:8080/api/system/ai/toggle \
  -H "X-API-Key: dev-secure-api-key-2024"
```

**Establecer Estado Específico:**
```bash
curl -X POST http://localhost:8080/api/system/ai/set \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-secure-api-key-2024" \
  -d '{"enabled": false}'
```

## Casos de Uso

### 1. Mantenimiento del Sistema
```bash
# Deshabilitar IA antes de realizar mantenimiento
curl -X POST http://localhost:8080/api/system/ai/disable
```

### 2. Cambio de Turno de Agentes
```bash
# Deshabilitar IA para que los agentes humanos tomen el control
curl -X POST http://localhost:8080/api/system/ai/disable

# Habilitar IA cuando los agentes terminen su turno
curl -X POST http://localhost:8080/api/system/ai/enable
```

### 3. Monitoreo del Estado
```bash
# Verificar el estado actual de la IA
curl http://localhost:8080/api/system/ai/status
```

### 4. Cambio Rápido de Estado
```bash
# Cambiar el estado actual (toggle)
curl -X POST http://localhost:8080/api/system/ai/toggle
```

## Logs del Sistema

El sistema registra todos los cambios de estado en los logs:

```
SystemConfigService: IA del sistema DESHABILITADA - Los usuarios serán atendidos por agentes humanos
ChatbotService: IA del sistema DESHABILITADA. Redirigiendo a agente humano...
```

## Notas Importantes

1. **Cambio Inmediato**: Los cambios de estado se aplican inmediatamente sin necesidad de reiniciar el servicio
2. **Persistencia**: El estado se mantiene en memoria durante la ejecución del servicio
3. **Estado Inicial**: La IA está habilitada por defecto al iniciar el sistema
4. **Logs**: Todos los cambios se registran para auditoría
5. **Usuarios COMPLETED**: Solo afecta a usuarios que ya completaron el flujo de registro
6. **Sin Variables de Entorno**: El control se hace únicamente a través de endpoints REST

## Pruebas Rápidas

### **Script de Prueba Automática**

Puedes crear un script bash para probar todo el flujo:

```bash
#!/bin/bash
echo "🧪 Probando Sistema de Control de IA..."
echo "======================================"

echo "1️⃣ Verificando estado inicial..."
curl -s http://localhost:8080/api/system/ai/status | jq '.'

echo -e "\n2️⃣ Deshabilitando IA..."
curl -s -X POST http://localhost:8080/api/system/ai/disable \
  -H "X-API-Key: dev-secure-api-key-2024" | jq '.'

echo -e "\n3️⃣ Verificando que se deshabilitó..."
curl -s http://localhost:8080/api/system/ai/status | jq '.'

echo -e "\n4️⃣ Habilitando IA..."
curl -s -X POST http://localhost:8080/api/system/ai/enable \
  -H "X-API-Key: dev-secure-api-key-2024" | jq '.'

echo -e "\n5️⃣ Estado final..."
curl -s http://localhost:8080/api/system/ai/status | jq '.'

echo -e "\n✅ Prueba completada!"
```

### **Prueba con Swagger UI**

Si prefieres usar la interfaz gráfica, ve a:
```
http://localhost:8080/swagger-ui.html
```

Y busca la sección "System Configuration" para probar todos los endpoints.

## 🔐 Seguridad

### **Autenticación por API Key**

Los endpoints de control del sistema están protegidos por autenticación de API Key:

- **Header requerido**: `X-API-Key`
- **Endpoint público**: Solo `/api/system/ai/status` (para verificar estado)
- **Endpoints protegidos**: Todos los que modifican el estado de la IA

### **Configuración de API Key**

#### **Desarrollo Local:**
```properties
system.api.key=dev-secure-api-key-2024
```

#### **Producción:**
```properties
system.api.key=${SYSTEM_API_KEY:default-secure-production-key}
```

### **Ejemplo de Uso con API Key:**

```bash
# Ver estado (público)
curl http://localhost:8080/api/system/ai/status

# Deshabilitar IA (requiere API Key)
curl -X POST http://localhost:8080/api/system/ai/disable \
  -H "X-API-Key: dev-secure-api-key-2024"

# Habilitar IA (requiere API Key)
curl -X POST http://localhost:8080/api/system/ai/enable \
  -H "X-API-Key: dev-secure-api-key-2024"
```

### **Recomendaciones de Seguridad:**

- **Cambiar la API Key por defecto** en producción
- **Usar HTTPS** en producción
- **Implementar rate limiting** para prevenir abuso
- **Monitorear logs** de acceso a endpoints protegidos
- **Rotar API Keys** periódicamente
- **Limitar acceso por IP** si es posible
