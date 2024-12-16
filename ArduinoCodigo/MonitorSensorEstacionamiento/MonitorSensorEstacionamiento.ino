#include <ESP8266WiFi.h>
#include "secrets.h"

const char* ssid = SECRET_SSID;
const char* password = SECRET_PASS;

const char* host = "api.thingspeak.com";      // Servidor de ThingSpeak
const char* apiKey = SECRET_WRITE_APIKEY;     // Clave de escritura
const char* readApiKey = SECRET_READ_APIKEY;  // Clave de lectura
const int channelID = SECRET_CH_ID;           // ID del canal

const int led = 2;        // LED principal
const int led2 = 14;      // LED2 controlado remotamente
const int trigger = 4;    // Trigger del sensor ultrasónico
const int echo = 5;       // Pin Echo del sensor ultrasónico

WiFiClient client;

void setup() {
  Serial.begin(115200);
  pinMode(led, OUTPUT);
  pinMode(led2, OUTPUT);
  pinMode(trigger, OUTPUT);
  pinMode(echo, INPUT);
  digitalWrite(trigger, LOW);
  digitalWrite(led2, LOW);  // LED2 esté apagado al inicio

  // Conexión WiFi
  Serial.print("Conectando a WiFi");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi conectado.");
}

void loop() {
  // Leer el estado del LED2 desde ThingSpeak
  String led2State = leerEstadoLED2();

  // Medir distancia
  long duration, distance;
  digitalWrite(trigger, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigger, LOW);
  duration = pulseIn(echo, HIGH, 30000); // Timeout 30ms
  distance = duration / 59;

  if (distance < 2) { distance = 2; }
  if (distance >= 2 && distance <= 400) {
    Serial.print("Distancia medida: ");
    Serial.print(distance);
    Serial.println(" cm");

    // LED1
    int ledState = (distance > 10) ? 1 : 0;

    // Si LED1 se apaga, también apaga el LED2
    if (ledState == 0) {
      digitalWrite(led2, LOW); // Apagar LED2
      led2State = "0";         // Actualizar el estado local de LED2
      enviarEstadoThingSpeak(distance, 0); // Actualizar estado en ThingSpeak
      Serial.println("Objeto detectado: LED1 y LED2 APAGADOS");
    } else {
      //Estado actual de LED2 en ThingSpeak
      enviarEstadoThingSpeak(distance, led2State.toInt());
    }

    // Actualizar el estado de LED1
    digitalWrite(led, ledState == 1 ? HIGH : LOW);
    Serial.println(ledState == 1 ? "LED1 ENCENDIDO" : "LED1 APAGADO");
  }

  // Valor recibido
  Serial.print("Estado LED2 recibido: ");
  Serial.println(led2State);

  // Estado del LED2
  if (led2State == "1") {
    digitalWrite(led2, HIGH); // Encender LED2
    Serial.println("LED2 ENCENDIDO");
    digitalWrite(led, LOW);
    int ledState = 0;
  } else {
    digitalWrite(led2, LOW); // Apagar LED2
    Serial.println("LED2 APAGADO");
  }

  delay(15500); // Esperar entre iteraciones para respetar los límites de ThingSpeak
}

// Lectura del estado del LED2
String leerEstadoLED2() {
  String led2State = "0"; // Valor por defecto
  if (client.connect(host, 80)) {
    String url = "/channels/2786195/fields/2/last.txt?api_key=";
    url += readApiKey;

    client.print(String("GET ") + url + " HTTP/1.1\r\n" +
                 "Host: " + host + "\r\n" +
                 "Connection: close\r\n\r\n");

    while (client.connected() || client.available()) {
      if (client.available()) {
        led2State = client.readStringUntil('\n'); // Respuesta
      }
    }
    client.stop();
  } else {
    Serial.println("Error al conectar para leer estado del LED2.");
  }
  led2State.trim(); 
  if (led2State != "0" && led2State != "1") {
    led2State = "0"; // Valor por defecto si no es válido
  }
  return led2State;
}

//Enviar distancia y estado del LED2 a ThingSpeak
void enviarEstadoThingSpeak(int distance, int ledState) {
  if (client.connect(host, 80)) {
    String url = "/update?api_key=";
    url += apiKey;
    url += "&field1=" + String(distance); // Enviar distancia en field1
    url += "&field2=" + String(ledState); // Enviar estado del LED2 en field2

    client.print(String("GET ") + url + " HTTP/1.1\r\n" +
                 "Host: " + host + "\r\n" +
                 "Connection: close\r\n\r\n");

    Serial.print("Enviando distancia: ");
    Serial.print(distance);
    Serial.print(" y estado LED2: ");
    Serial.println(ledState);

    client.stop();
  } else {
    Serial.println("Error al conectar con ThingSpeak.");
  }
}
