#!/bin/bash

# Script para probar específicamente el flujo de términos de privacidad

echo "=== PRUEBA DE FLUJO DE TÉRMINOS DE PRIVACIDAD ==="

# URL del webhook
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular flujo completo para verificar que SIEMPRE se valide la política de privacidad
echo "1. Primer mensaje - Usuario nuevo..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Hola",
    "senderName": "Santiago"
  }'

echo -e "\n\n2. Segundo mensaje - Debería pedir términos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Sí",
    "senderName": "Santiago"
  }'

echo -e "\n\n3. Tercer mensaje - Debería pedir nombre..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Me llamo Santiago",
    "senderName": "Santiago"
  }'

echo -e "\n\n4. Cuarto mensaje - Debería pedir ciudad..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Vivo en Bogotá",
    "senderName": "Santiago"
  }'

echo -e "\n\n5. Quinto mensaje - Debería confirmar datos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Sí",
    "senderName": "Santiago"
  }'

echo -e "\n\n6. Sexto mensaje - Debería pedir términos de privacidad..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573227281752",
    "text": "Hola de nuevo",
    "senderName": "Santiago"
  }'

echo -e "\n\n=== FIN DE PRUEBAS ==="
echo "Revisa los logs para verificar que SIEMPRE se valide la política de privacidad" 