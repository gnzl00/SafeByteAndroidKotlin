package com.safebyte

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Bienvenido a Food DNA!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cerrar sesion")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Nuestra app te ayuda a elegir productos y comidas mas seguras segun tus alergias alimentarias.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Testimonios",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        TestimonialCard(
            imageResName = "mujer_celiaca",
            title = "Lucia, 27 anos (celiaca)",
            quote = "Ser celiaca no es solo evitar el pan. Food DNA me ayudaria a sentirme segura y confiada cada vez que como algo nuevo."
        )
        Spacer(Modifier.height(12.dp))
        TestimonialCard(
            imageResName = "hombre_frutos_secos",
            title = "Diego, 34 anos (alergico a frutos secos)",
            quote = "Una reaccion alergica puede cambiarte el dia o la vida. Esta app me daria tranquilidad y evitaria leer cada etiqueta durante minutos."
        )

        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Como usar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text("1) Ve a Config y marca tus alergenos.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("2) En Scanner, escanea un codigo de barras o escribe el numero.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("3) En Comidas, filtra recetas y revisa alergenos.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TestimonialCard(imageResName: String, title: String, quote: String) {
    val resId = drawableId(imageResName)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(12.dp)) {
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = title,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text("\"$quote\"", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
