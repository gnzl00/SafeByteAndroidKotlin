package com.safebyte

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF4F7F6), Color(0xFFE9F5E9))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E5631)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Bienvenido a Food DNA!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF6FFF9),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Nuestra aplicacion esta disenada para ofrecerte comidas seguras basadas en tus alergias alimentarias.",
                        color = Color.White.copy(alpha = 0.94f)
                    )
                }
            }

            Text(
                "Testimonios",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF1E5631),
                fontWeight = FontWeight.Bold
            )

            BoxWithConstraints {
                val twoColumns = maxWidth > 880.dp
                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TestimonialCard(
                            imageResId = R.drawable.mujer_celiaca,
                            title = "Lucia, 27 anos (celiaca)",
                            quote = "Ser celiaca no es solo evitar el pan. Comer fuera o elegir productos es una batalla constante. Food DNA me da seguridad.",
                            modifier = Modifier.weight(1f)
                        )
                        TestimonialCard(
                            imageResId = R.drawable.hombre_frutos_secos,
                            title = "Diego, 34 anos (alergico a frutos secos)",
                            quote = "Una reaccion alergica puede cambiarte el dia. Esta app me da tranquilidad y evita leer etiquetas durante minutos.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TestimonialCard(
                            imageResId = R.drawable.mujer_celiaca,
                            title = "Lucia, 27 anos (celiaca)",
                            quote = "Ser celiaca no es solo evitar el pan. Comer fuera o elegir productos es una batalla constante. Food DNA me da seguridad."
                        )
                        TestimonialCard(
                            imageResId = R.drawable.hombre_frutos_secos,
                            title = "Diego, 34 anos (alergico a frutos secos)",
                            quote = "Una reaccion alergica puede cambiarte el dia. Esta app me da tranquilidad y evita leer etiquetas durante minutos."
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F5E9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Como usar",
                        color = Color(0xFF1E5631),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text("1) Ve a Configuracion de usuario y marca tus alergenos.", color = Color(0xFF274537))
                    Text("2) En Scanner, escanea un codigo de barras o escribe el numero.", color = Color(0xFF274537))
                    Text("3) En Comidas, busca recetas y revisa alergenos.", color = Color(0xFF274537))
                    Text("4) En IANutri, genera ideas y guias de cocina seguras.", color = Color(0xFF274537))
                }
            }
        }
    }
}

@Composable
private fun TestimonialCard(
    @DrawableRes imageResId: Int,
    title: String,
    quote: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F9F1)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(imageResId),
                contentDescription = title,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
            )
            Column(Modifier.weight(1f)) {
                Text(title, color = Color(0xFF2A5934), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("\"$quote\"", color = Color(0xFF444444))
            }
        }
    }
}
