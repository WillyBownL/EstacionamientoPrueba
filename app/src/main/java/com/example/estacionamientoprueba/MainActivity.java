package com.example.estacionamientoprueba;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.DataAsyncHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    TextView estadoLed, estadoLed2;
    private static final String TAG = "MainActivity";

    // Variable global para almacenar el último valor de field1 (LED1)
    private String ultimoValorField1 = "0"; // Valor inicial predeterminado

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar los TextView
        estadoLed = findViewById(R.id.estado_led);
        estadoLed2 = findViewById(R.id.estado_led2);

        // Leer el estado inicial de los LEDs
        leerEstadoLeds();
    }

    public void Reservar(View view) {
        // Usa el último valor guardado de field1 y actualiza field2 a 1
        String url = "https://api.thingspeak.com/update?api_key=BA1O3YURJ40P9H22&field1=" + ultimoValorField1 + "&field2=1";
        enviarPeticion(url);
    }

    public void QuitarReserva(View view) {
        // Usa el último valor guardado de field1 y actualiza field2 a 0
        String url = "https://api.thingspeak.com/update?api_key=BA1O3YURJ40P9H22&field1=" + ultimoValorField1 + "&field2=0";
        enviarPeticion(url);
    }

    private void enviarPeticion(String url) {
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new DataAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200) {
                    Toast.makeText(MainActivity.this, "Actualización exitosa", Toast.LENGTH_SHORT).show();
                    leerEstadoLeds(); // Actualizar los estados después de enviar
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "Error al actualizar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Método para actualizar manualmente los estados
    public void ActualizarEstados(View view) {
        Toast.makeText(this, "Actualizando estados...", Toast.LENGTH_SHORT).show();
        leerEstadoLeds();
    }

    // Método para leer el estado de los LEDs desde ThingSpeak
    private void leerEstadoLeds() {
        String url = "https://api.thingspeak.com/channels/2786195/feeds/last.json?api_key=FBNV181569PY4N5E&t=" + System.currentTimeMillis();
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(url, new DataAsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode == 200) {
                    try {
                        String response = new String(responseBody);
                        Log.d(TAG, "Respuesta de ThingSpeak: " + response);

                        // Parsear el JSON para obtener los valores de field1 y field2
                        JSONObject json = new JSONObject(response);
                        String field1 = json.optString("field1", "").trim();
                        String field2 = json.optString("field2", "").trim();

                        // Verificar si los campos son nulos, vacíos o inválidos
                        if (field1.equals("") || field1.equalsIgnoreCase("null") ||
                                field2.equals("") || field2.equalsIgnoreCase("null")) {
                            Log.e(TAG, "Error: Datos inválidos recibidos");
                            Toast.makeText(MainActivity.this, "Error: Datos NULL o inválidos recibidos", Toast.LENGTH_SHORT).show();

                            // Actualizar los TextView con mensajes de error
                            runOnUiThread(() -> {
                                estadoLed.setText("Disponibilidad: Error - Dato NULL");
                                estadoLed2.setText("Reserva: Error - Dato NULL");
                            });
                            return; // Terminar la ejecución del método
                        }

                        // Guardar el último valor de field1 (distancia)
                        ultimoValorField1 = field1;

                        // Convertir field1 a entero de forma segura
                        int distancia = Integer.parseInt(field1);

                        // Lógica para encender/apagar LED1 según distancia
                        String estadoLedValue = (distancia > 10) ? "Disponible" : "Ocupado";
                        String estadoLed2Value = field2.equals("1") ? "Reservado" : "No reservado";

                        // Mensajes de depuración
                        Log.d(TAG, "Distancia recibida: " + distancia);
                        Log.d(TAG, "Estado LED: " + estadoLedValue);
                        Log.d(TAG, "Estado LED2: " + estadoLed2Value);

                        // Actualizar los TextView en el hilo principal
                        runOnUiThread(() -> {
                            estadoLed.setText("Disponibilidad: " + estadoLedValue);
                            estadoLed2.setText("Reserva: " + estadoLed2Value);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar los datos", e);
                        Toast.makeText(MainActivity.this, "Error al procesar los datos", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "Error al leer estados", error);
                Toast.makeText(MainActivity.this, "Error al leer estados", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
