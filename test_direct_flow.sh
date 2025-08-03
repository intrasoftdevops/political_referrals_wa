#!/bin/bash

# Script para probar el flujo directo sin confirmación innecesaria

echo "=== PRUEBA DE FLUJO DIRECTO ==="

# URL del webhook
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular flujo completo para verificar que va directo a política de privacidad
echo "1. Primer mensaje - Usuario nuevo..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281756",
    "text": "Hola",
    "senderName": "Ana"
  }'

echo -e "\n\n2. Segundo mensaje - Nombre..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281756",
    "text": "Ana",
    "senderName": "Ana"
  }'

echo -e "\n\n3. Tercer mensaje - Ciudad (debería ir directo a política de privacidad)..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281756",
    "text": "Bogota",
    "senderName": "Ana"
  }'

echo -e "\n\n=== PRUEBA COMPLETADA ==="
echo "Verifica en los logs que:"
echo "1. Después de ingresar la ciudad, va DIRECTAMENTE a política de privacidad"
echo "2. NO pide confirmación de datos innecesaria"
echo "3. El mensaje de política es el correcto" 