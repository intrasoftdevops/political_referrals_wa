# 🔐 Configuración de GitHub Secrets para Notificaciones WhatsApp

## 📱 **Secretos Requeridos**

Para que el pipeline CI/CD envíe notificaciones de WhatsApp, debes configurar estos secretos en tu repositorio de GitHub:

### **1. Configurar Secretos en GitHub:**

1. **Ve a tu repositorio** en GitHub
2. **Settings** → **Secrets and variables** → **Actions**
3. **New repository secret**

### **2. Secretos a Configurar:**

#### **🔑 WHATSAPP_NOTIFICATION_PHONE**
- **Descripción:** Número de teléfono para recibir notificaciones
- **Valor:** `573227281752` (tu número)
- **Formato:** Código de país + número (sin espacios ni caracteres especiales)

#### **🔑 WHATSAPP_GROUP_ID** (Opcional)
- **Descripción:** ID del grupo de WhatsApp para notificaciones del equipo
- **Valor:** Número del grupo (ej: `573001234567`)
- **Nota:** Si no tienes grupo, déjalo vacío

### **3. Cómo Obtener el ID del Grupo:**

1. **Crea un grupo** en WhatsApp
2. **Agrega tu número de Wati** al grupo
3. **El ID del grupo** es el número de teléfono del grupo

## 🚀 **Configuración Automática:**

### **Opción 1: Usar el Script (Recomendado)**
```bash
# Ejecutar el script de configuración
./scripts/setup-whatsapp-notifications.sh
```

### **Opción 2: Configuración Manual**
1. **Edita** `src/main/resources/application-notifications.properties`
2. **Configura** tu número o grupo
3. **NO subas** este archivo al repositorio

## 📋 **Prioridad de Notificaciones:**

1. **GitHub Secrets** (prioridad alta) - Para producción
2. **Archivo local** (fallback) - Para desarrollo local

## 🔍 **Verificar Configuración:**

Después de configurar los secretos:

1. **Haz commit y push** de los cambios
2. **El pipeline se ejecutará** automáticamente
3. **Recibirás notificaciones** en tu WhatsApp

## ⚠️ **Notas Importantes:**

- **NO subas** `application-notifications.properties` al repositorio
- **Usa siempre** GitHub Secrets para producción
- **El archivo local** es solo para desarrollo
- **Las notificaciones** se envían solo cuando `notificationsEnabled=true`

## 🎯 **Ejemplo de Uso:**

```bash
# Configurar secretos en GitHub
WHATSAPP_NOTIFICATION_PHONE=573227281752
WHATSAPP_GROUP_ID=573001234567

# Hacer commit y push
git add .
git commit -m "Configure WhatsApp notifications"
git push

# El pipeline enviará notificaciones automáticamente
``` 