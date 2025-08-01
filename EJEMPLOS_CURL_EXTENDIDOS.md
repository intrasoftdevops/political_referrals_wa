# 🚀 Ejemplos CURL para Extracción Extendida (lastname + state)

## 📋 Nuevos Campos Extraídos

- **`name`**: Nombre (sin apellido, sin títulos)
- **`lastname`**: Apellido(s) completo(s)
- **`city`**: Ciudad colombiana específica
- **`state`**: Departamento/Estado colombiano
- **`acceptsTerms`**: Si acepta términos explícitamente
- **`referredByPhone`**: Número +57XXXXXXXXX
- **`referralCode`**: Código alfanumérico de 8 dígitos

## 🧪 Ejemplos de Prueba

### 1. **Extracción Completa con Nombre y Apellido**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573001111111",
    "text": "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, Atlántico, acepto sus términos, vengo por +573001234567"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "Miguel",
  "lastname": "Rodríguez",
  "city": "Barranquilla",
  "state": "Atlántico",
  "acceptsTerms": true,
  "referredByPhone": "+573001234567",
  "confidence": 0.95
}
```

### 2. **Extracción con Nombre Completo y Departamento**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573002222222",
    "text": "Hola, soy María García de Medellín, Antioquia"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "María",
  "lastname": "García",
  "city": "Medellín",
  "state": "Antioquia",
  "acceptsTerms": null,
  "confidence": 0.85
}
```

### 3. **Solo Nombre y Apellido**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573003333333",
    "text": "Me llamo Juan Carlos López"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "Juan Carlos",
  "lastname": "López",
  "city": null,
  "state": null,
  "acceptsTerms": null,
  "confidence": 0.9
}
```

### 4. **Solo Ciudad y Departamento**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573004444444",
    "text": "Soy de Cali, Valle del Cauca"
  }'
```

**Resultado Esperado:**
```json
{
  "name": null,
  "lastname": null,
  "city": "Cali",
  "state": "Valle del Cauca",
  "acceptsTerms": null,
  "confidence": 0.8
}
```

### 5. **Extracción con Títulos Profesionales**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573005555555",
    "text": "Hola! Soy Ing. Laura Martínez de Bucaramanga, Santander, acepto los términos"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "Laura",
  "lastname": "Martínez",
  "city": "Bucaramanga",
  "state": "Santander",
  "acceptsTerms": true,
  "confidence": 0.95
}
```

### 6. **Extracción Compleja con Todos los Datos**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573006666666",
    "text": "Hola! Soy Dr. Ana María López de Pereira, Risaralda, acepto completamente los términos y condiciones, vengo referido por mi amigo +573001234567 con código REF12345"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "Ana María",
  "lastname": "López",
  "city": "Pereira",
  "state": "Risaralda",
  "acceptsTerms": true,
  "referredByPhone": "+573001234567",
  "referralCode": "REF12345",
  "confidence": 0.98
}
```

### 7. **Extracción con Nombres Compuestos**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573007777777",
    "text": "Soy José Luis Ramírez de Cartagena, Bolívar"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "José Luis",
  "lastname": "Ramírez",
  "city": "Cartagena",
  "state": "Bolívar",
  "acceptsTerms": null,
  "confidence": 0.9
}
```

### 8. **Extracción con Apellidos Compuestos**
```bash
curl -X POST http://localhost:8081/api/wati-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "+573008888888",
    "text": "Hola! Soy Carlos de la Rosa de Manizales, Caldas"
  }'
```

**Resultado Esperado:**
```json
{
  "name": "Carlos",
  "lastname": "de la Rosa",
  "city": "Manizales",
  "state": "Caldas",
  "acceptsTerms": null,
  "confidence": 0.85
}
```

## 🎯 Respuestas del Bot

### **Caso de Extracción Completa:**
```
¡Perfecto! Confirmamos tus datos: Miguel Rodríguez, de Barranquilla, Atlántico. ¿Es correcto? (Sí/No)
```

### **Caso de Extracción Parcial:**
```
¡Hola Miguel Rodríguez! Veo que eres de Barranquilla, Atlántico. Para continuar, necesito que aceptes los términos. ¿Aceptas? (Sí/No)
```

### **Caso de Solo Nombre:**
```
¡Hola Miguel Rodríguez! ¿De qué ciudad eres?
```

### **Caso de Solo Ciudad:**
```
¡Hola! Veo que eres de Barranquilla, Atlántico. ¿Cuál es tu nombre?
```

## 🚀 Ejecutar Pruebas

Para ejecutar todas las pruebas automáticamente:

```bash
./test_gemini_extended_integration.sh
```

## 📊 Departamentos Colombianos Soportados

- **Antioquia**: Medellín, Sabaneta, Envigado
- **Atlántico**: Barranquilla
- **Bolívar**: Cartagena
- **Boyacá**: Tunja
- **Caldas**: Manizales, La Dorada
- **Caquetá**: Florencia
- **Casanare**: Yopal
- **Cauca**: Popayán
- **Cesar**: Valledupar
- **Chocó**: Quibdó
- **Córdoba**: Montería
- **Cundinamarca**: Bogotá
- **Guainía**: Inírida
- **Guaviare**: San José del Guaviare
- **La Guajira**: Riohacha
- **Meta**: Villavicencio
- **Nariño**: Pasto
- **Putumayo**: Mocoa, La Dorada
- **Quindío**: Armenia
- **Risaralda**: Pereira
- **San Andrés y Providencia**: San Andrés
- **Santander**: Bucaramanga
- **Tolima**: Ibagué
- **Valle del Cauca**: Cali
- **Vaupés**: Mitú
- **Vichada**: Puerto Carreño
- **Amazonas**: Leticia
- **Arauca**: Arauca 