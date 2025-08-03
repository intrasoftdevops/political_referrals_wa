#!/bin/bash

# Script para probar la corrección específica de datos

echo "=== PRUEBA DE CORRECCIÓN ESPECÍFICA DE DATOS ==="

# URL del webhook
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular flujo completo para verificar que la corrección funciona correctamente
echo "1. Primer mensaje - Usuario nuevo..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Hola",
    "senderName": "Ana"
  }'

echo -e "\n\n2. Segundo mensaje - Nombre..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Ana",
    "senderName": "Ana"
  }'

echo -e "\n\n3. Tercer mensaje - Ciudad incorrecta..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Medellin",
    "senderName": "Ana"
  }'

echo -e "\n\n4. Cuarto mensaje - Confirmación de datos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Sí",
    "senderName": "Ana"
  }'

echo -e "\n\n5. Quinto mensaje - Corrigiendo ciudad específicamente..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "No, soy de Bogota",
    "senderName": "Ana"
  }'

echo -e "\n\n6. Sexto mensaje - Nueva ciudad..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Bogota",
    "senderName": "Ana"
  }'

echo -e "\n\n7. Séptimo mensaje - Confirmación final..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281754",
    "text": "Sí",
    "senderName": "Ana"
  }'

echo -e "\n\n=== PRUEBA COMPLETADA ==="
echo "Verifica en los logs que:"
echo "1. El usuario pudo corregir específicamente la ciudad"
echo "2. No se pidió el nombre cuando el usuario quería corregir la ciudad"
echo "3. El flujo continuó correctamente después de la corrección" 