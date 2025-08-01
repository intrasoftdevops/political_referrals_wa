#!/bin/bash

echo "🧪 Probando Extracción Extendida con Gemini AI (lastname + state)"
echo "================================================================"

BASE_URL="http://localhost:8081/api/wati-webhook"

# Función para hacer petición CURL
test_extraction() {
    local test_name="$1"
    local phone="$2"
    local message="$3"
    
    echo ""
    echo "📝 Test: $test_name"
    echo "📱 Teléfono: $phone"
    echo "💬 Mensaje: $message"
    echo "🔄 Enviando..."
    
    response=$(curl -s -X POST "$BASE_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventType\": \"message\",
            \"type\": \"text\",
            \"waId\": \"$phone\",
            \"text\": \"$message\"
        }")
    
    echo "✅ Respuesta: $response"
    echo "---"
}

echo ""
echo "🚀 Iniciando pruebas de extracción extendida..."

# Test 1: Extracción completa con nombre y apellido
test_extraction "Extracción Completa con Nombre y Apellido" \
    "+573001111111" \
    "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, Atlántico, acepto sus términos, vengo por +573001234567"

# Test 2: Extracción con nombre completo y departamento
test_extraction "Extracción con Nombre Completo y Departamento" \
    "+573002222222" \
    "Hola, soy María García de Medellín, Antioquia"

# Test 3: Solo nombre y apellido
test_extraction "Solo Nombre y Apellido" \
    "+573003333333" \
    "Me llamo Juan Carlos López"

# Test 4: Solo ciudad y departamento
test_extraction "Solo Ciudad y Departamento" \
    "+573004444444" \
    "Soy de Cali, Valle del Cauca"

# Test 5: Extracción con títulos profesionales
test_extraction "Extracción con Títulos Profesionales" \
    "+573005555555" \
    "Hola! Soy Ing. Laura Martínez de Bucaramanga, Santander, acepto los términos"

# Test 6: Extracción compleja con todos los datos
test_extraction "Extracción Compleja con Todos los Datos" \
    "+573006666666" \
    "Hola! Soy Dr. Ana María López de Pereira, Risaralda, acepto completamente los términos y condiciones, vengo referido por mi amigo +573001234567 con código REF12345"

# Test 7: Extracción con nombres compuestos
test_extraction "Extracción con Nombres Compuestos" \
    "+573007777777" \
    "Soy José Luis Ramírez de Cartagena, Bolívar"

# Test 8: Extracción con apellidos compuestos
test_extraction "Extracción con Apellidos Compuestos" \
    "+573008888888" \
    "Hola! Soy Carlos de la Rosa de Manizales, Caldas"

echo ""
echo "🎉 Pruebas completadas!"
echo "📊 Revisa los logs de la aplicación para ver los resultados de Gemini AI" 