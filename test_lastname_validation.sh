#!/bin/bash

# Script para probar la validación y extracción de apellidos
# Simula diferentes escenarios de nombres completos de WhatsApp

echo "🧪 Probando validación y extracción de apellidos..."
echo "=================================================="

# Función para enviar mensaje al webhook
send_message() {
    local phone="$1"
    local sender_name="$2"
    local message="$3"
    
    echo "📱 Enviando mensaje de '$sender_name' ($phone): '$message'"
    
    curl -X POST http://localhost:8081/wati/webhook \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"test-$(date +%s)\",
            \"text\": \"$message\",
            \"waId\": \"$phone\",
            \"senderName\": \"$sender_name\",
            \"eventType\": \"message\",
            \"type\": \"text\"
        }" \
        -s | jq '.' 2>/dev/null || echo "Respuesta recibida"
    
    echo "---"
    sleep 2
}

# Casos de prueba con diferentes nombres completos
echo "🔍 Caso 1: Nombre completo con apellido"
send_message "573227281753" "Juan Pérez" "Hola"

echo "🔍 Caso 2: Solo nombre (sin apellido)"
send_message "573227281754" "María" "Hola"

echo "🔍 Caso 3: Nombre compuesto con apellido"
send_message "573227281755" "Juan Carlos Rodríguez" "Hola"

echo "🔍 Caso 4: Nombre con apellidos compuestos"
send_message "573227281756" "Ana Sofía Pérez González" "Hola"

echo "🔍 Caso 5: Nombre inválido (WhatsApp)"
send_message "573227281757" "WhatsApp" "Hola"

echo "🔍 Caso 6: Nombre genérico"
send_message "573227281758" "Usuario" "Hola"

echo "✅ Pruebas completadas. Revisa los logs para ver los resultados." 