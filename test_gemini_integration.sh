#!/bin/bash

# Script de prueba para la integración de Gemini AI
# Asegúrate de tener la aplicación corriendo en localhost:8081

echo "🧪 Probando Integración de Gemini AI - Inputs Inteligentes"
echo "=========================================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para hacer requests
test_case() {
    local test_name="$1"
    local message="$2"
    local expected_keyword="$3"
    
    echo -e "\n${BLUE}📝 Test: $test_name${NC}"
    echo "Mensaje: $message"
    
    response=$(curl -s -X POST http://localhost:8081/webhook/wati \
        -H "Content-Type: application/json" \
        -d "{\"message\": \"$message\"}")
    
    if [[ $response == *"$expected_keyword"* ]]; then
        echo -e "${GREEN}✅ Éxito: Respuesta contiene '$expected_keyword'${NC}"
        echo "Respuesta: $response"
    else
        echo -e "${RED}❌ Fallo: Respuesta no contiene '$expected_keyword'${NC}"
        echo "Respuesta: $response"
    fi
}

# Verificar si la aplicación está corriendo
echo -e "${YELLOW}🔍 Verificando que la aplicación esté corriendo...${NC}"
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Aplicación corriendo en localhost:8081${NC}"
else
    echo -e "${RED}❌ Error: La aplicación no está corriendo en localhost:8081${NC}"
    echo "Ejecuta: mvn spring-boot:run"
    exit 1
fi

echo -e "\n${YELLOW}🚀 Iniciando pruebas de casos de uso...${NC}"

# Caso 1: Extracción completa exitosa
test_case \
    "Registro Completo en un Mensaje" \
    "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos, vengo por +573001234567" \
    "Dr. Miguel Rodríguez"

# Caso 2: Manejo de ambigüedad
test_case \
    "Detección de Ambigüedad" \
    "Vivo en La Dorada y trabajo en educación" \
    "La Dorada"

# Caso 3: Extracción parcial
test_case \
    "Extracción Parcial" \
    "Hola, me llamo Ana" \
    "Ana"

# Caso 4: Aclaración de ambigüedad
test_case \
    "Aclaración de Ambigüedad" \
    "La de Caldas" \
    "Caldas"

# Caso 5: Corrección natural
test_case \
    "Corrección Natural" \
    "Me equivoqué, no soy de Medellín sino de Envigado" \
    "Envigado"

# Caso 6: Número de referido
test_case \
    "Número de Referido" \
    "Hola, vengo referido por +573001234567" \
    "referido"

# Caso 7: Código de referido
test_case \
    "Código de Referido" \
    "Hola, vengo referido por: ABC12345" \
    "ABC12345"

# Caso 8: Fallback tradicional
test_case \
    "Fallback Tradicional" \
    "Hola" \
    "Reset a la Política"

echo -e "\n${GREEN}🎉 Pruebas completadas!${NC}"
echo -e "${YELLOW}📊 Revisa los logs de la aplicación para ver los detalles de extracción${NC}"

# Mostrar comandos útiles
echo -e "\n${BLUE}🔧 Comandos útiles:${NC}"
echo "• Ver logs: tail -f app.log"
echo "• Reiniciar app: mvn spring-boot:run"
echo "• Ejecutar tests: mvn test"
echo "• Verificar health: curl http://localhost:8081/actuator/health" 