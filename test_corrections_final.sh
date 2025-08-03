#!/bin/bash

# Script para probar las correcciones finales

echo "=== PRUEBA DE CORRECCIONES FINALES ==="

# URL del webhook
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular flujo completo para verificar las correcciones
echo "1. Primer mensaje - Usuario nuevo..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Hola",
    "senderName": "Carlos"
  }'

echo -e "\n\n2. Segundo mensaje - Nombre..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Carlos",
    "senderName": "Carlos"
  }'

echo -e "\n\n3. Tercer mensaje - Ciudad incorrecta..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Cali",
    "senderName": "Carlos"
  }'

echo -e "\n\n4. Cuarto mensaje - Confirmación de datos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Sí",
    "senderName": "Carlos"
  }'

echo -e "\n\n5. Quinto mensaje - Corrigiendo ciudad con extracción automática..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "No, soy de Bogota",
    "senderName": "Carlos"
  }'

echo -e "\n\n6. Sexto mensaje - Confirmación de datos corregidos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Sí",
    "senderName": "Carlos"
  }'

echo -e "\n\n7. Séptimo mensaje - Aceptando términos (debería mostrar mensaje correcto)..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281755",
    "text": "Sí",
    "senderName": "Carlos"
  }'

echo -e "\n\n=== PRUEBA COMPLETADA ==="
echo "Verifica en los logs que:"
echo "1. Se extrajo automáticamente 'Bogota' del mensaje 'No, soy de Bogota'"
echo "2. El mensaje de política de privacidad es el correcto"
echo "3. El flujo continuó correctamente después de las correcciones" 