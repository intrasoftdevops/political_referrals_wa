#!/bin/bash

# Script para configurar notificaciones de WhatsApp
# ================================================

echo "🚀 Configurando Notificaciones de WhatsApp para CI/CD"
echo "=================================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar mensajes de error
show_error() {
    echo -e "${RED}❌ Error: $1${NC}"
}

# Función para mostrar mensajes de éxito
show_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Función para mostrar mensajes de info
show_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Función para mostrar mensajes de warning
show_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo ""
show_info "Este script te ayudará a configurar las notificaciones de WhatsApp para tu pipeline CI/CD"
echo ""

# Verificar si existe el archivo de configuración
CONFIG_FILE="src/main/resources/application-notifications.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    show_error "No se encontró el archivo de configuración: $CONFIG_FILE"
    exit 1
fi

echo "📱 Opciones de configuración:"
echo "1. Grupo de WhatsApp (recomendado para equipos)"
echo "2. Números de teléfono específicos"
echo "3. Solo habilitar/deshabilitar notificaciones"
echo ""

read -p "Selecciona una opción (1-3): " choice

case $choice in
    1)
        echo ""
        show_info "Configurando notificaciones para grupo de WhatsApp"
        echo ""
        echo "📋 Para obtener el ID del grupo:"
        echo "   - Crea un grupo en WhatsApp"
        echo "   - Agrega tu número de Wati al grupo"
        echo "   - El ID del grupo es el número de teléfono del grupo"
        echo ""
        read -p "Ingresa el ID del grupo (ej: 573001234567): " group_id
        
        if [[ -n "$group_id" ]]; then
            # Actualizar configuración
            sed -i.bak "s/^wati.notification.group.id=.*/wati.notification.group.id=$group_id/" "$CONFIG_FILE"
            sed -i.bak "s/^wati.notification.phones=.*/wati.notification.phones=/" "$CONFIG_FILE"
            sed -i.bak "s/^wati.notification.enabled=.*/wati.notification.enabled=true/" "$CONFIG_FILE"
            
            show_success "Grupo configurado exitosamente: $group_id"
            show_info "Las notificaciones se enviarán al grupo de WhatsApp"
        else
            show_error "ID de grupo no válido"
            exit 1
        fi
        ;;
        
    2)
        echo ""
        show_info "Configurando notificaciones para números específicos"
        echo ""
        echo "📋 Formato: números separados por coma (ej: 573001234567,573007654321)"
        echo "   - Incluye el código de país"
        echo "   - Sin espacios ni caracteres especiales"
        echo ""
        read -p "Ingresa los números de teléfono: " phone_numbers
        
        if [[ -n "$phone_numbers" ]]; then
            # Actualizar configuración
            sed -i.bak "s/^wati.notification.group.id=.*/wati.notification.group.id=/" "$CONFIG_FILE"
            sed -i.bak "s/^wati.notification.phones=.*/wati.notification.phones=$phone_numbers/" "$CONFIG_FILE"
            sed -i.bak "s/^wati.notification.enabled=.*/wati.notification.enabled=true/" "$CONFIG_FILE"
            
            show_success "Números configurados exitosamente: $phone_numbers"
            show_info "Las notificaciones se enviarán a los números especificados"
        else
            show_error "Números de teléfono no válidos"
            exit 1
        fi
        ;;
        
    3)
        echo ""
        show_info "Solo configurando habilitación de notificaciones"
        echo ""
        read -p "¿Habilitar notificaciones? (y/n): " enable_notifications
        
        if [[ "$enable_notifications" =~ ^[Yy]$ ]]; then
            sed -i.bak "s/^wati.notification.enabled=.*/wati.notification.enabled=true/" "$CONFIG_FILE"
            show_success "Notificaciones habilitadas"
        else
            sed -i.bak "s/^wati.notification.enabled=.*/wati.notification.enabled=false/" "$CONFIG_FILE"
            show_success "Notificaciones deshabilitadas"
        fi
        ;;
        
    *)
        show_error "Opción no válida"
        exit 1
        ;;
esac

echo ""
show_info "Resumen de la configuración actual:"
echo "======================================"

# Mostrar configuración actual
echo "Habilitado: $(grep 'wati.notification.enabled=' "$CONFIG_FILE" | cut -d'=' -f2)"
echo "Grupo ID: $(grep 'wati.notification.group.id=' "$CONFIG_FILE" | cut -d'=' -f2)"
echo "Teléfonos: $(grep 'wati.notification.phones=' "$CONFIG_FILE" | cut -d'=' -f2)"

echo ""
show_success "¡Configuración completada!"
echo ""
show_info "Próximos pasos:"
echo "1. Haz commit de los cambios: git add . && git commit -m 'Add WhatsApp notifications'"
echo "2. Haz push: git push"
echo "3. El pipeline enviará notificaciones automáticamente"
echo ""
show_warning "Nota: Asegúrate de que tu API de Wati esté configurada correctamente" 