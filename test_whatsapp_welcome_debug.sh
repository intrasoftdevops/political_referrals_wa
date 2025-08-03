#!/bin/bash

# Script para probar el webhook de WhatsApp y debuggear el mensaje de bienvenida repetido

echo "=== PRUEBA DE WEBHOOK WHATSAPP - DEBUG MENSAJE BIENVENIDA ==="

# URL del webhook (ajusta según tu configuración)
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular mensaje de nuevo usuario
echo "1. Probando mensaje de nuevo usuario..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573001234567",
    "text": "Hola",
    "senderName": "Juan Pérez"
  }'

echo -e "\n\n2. Probando segundo mensaje del mismo usuario..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573001234567",
    "text": "Mi nombre es Juan",
    "senderName": "Juan Pérez"
  }'

echo -e "\n\n3. Probando tercer mensaje del mismo usuario..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573001234567",
    "text": "Vivo en Bogotá",
    "senderName": "Juan Pérez"
  }'

echo -e "\n\n4. Probando mensaje con código de referido..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573009876543",
    "text": "Hola, vengo referido por:TESTCODE",
    "senderName": "María García"
  }'

echo -e "\n\n=== FIN DE PRUEBAS ==="
echo "Revisa los logs del servidor para ver el debugging detallado" 