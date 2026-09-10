package com.htogo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.htogo.app.ui.theme.HToGoColors
import com.htogo.app.ui.theme.HToGoTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AvisoPrivacidadScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        containerColor = HToGoColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Aviso de Privacidad",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HToGoColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = HToGoColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HToGoColors.Background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            DocHeader()
            Spacer(Modifier.height(18.dp))

            Seccion(titulo = "1. Identidad y domicilio del responsable") {
                Parrafo(
                    "El responsable del tratamiento de sus datos personales es el equipo desarrollador del " +
                        "proyecto H2ToGo, prototipo académico desarrollado en el Instituto Politécnico " +
                        "Nacional (IPN), Escuela Superior de Cómputo (ESCOM). Para cualquier asunto " +
                        "relacionado con este Aviso, puede contactarnos a través del correo electrónico " +
                        "gmarquezr1900@alumno.ipn.mx o btrejoh1900@alumno.ipn.mx."
                )
            }

            Seccion(titulo = "2. Datos personales que recabamos") {
                Parrafo(
                    "H2ToGo recaba los siguientes datos personales para la prestación del servicio de " +
                        "gestión y entrega de agua potable:"
                )
                Spacer(Modifier.height(4.dp))
                Bullets(
                    listOf(
                        "Nombre completo, correo electrónico y número de teléfono (registro y contacto).",
                        "Domicilio de entrega e indicaciones adicionales (coordenadas GPS del punto de entrega).",
                        "Ubicación geográfica en tiempo real del repartidor durante el estado activo de una entrega.",
                        "Historial de pedidos, cantidades y precios acordados.",
                        "Contraseña, almacenada exclusivamente en formato de hash cifrado; nunca en texto plano."
                    )
                )
                Spacer(Modifier.height(8.dp))
                CardDatoSensible(
                    titulo = "Dato sensible",
                    texto = "La ubicación GPS en tiempo real del repartidor constituye un dato de " +
                        "geolocalización continua. Su tratamiento requiere de su consentimiento expreso, " +
                        "el cual se solicita de forma separada al momento de otorgar el permiso de " +
                        "ubicación en su dispositivo."
                )
            }

            Seccion(titulo = "3. Finalidades del tratamiento") {
                Parrafo("Sus datos se utilizan exclusivamente para las siguientes finalidades:")
                Spacer(Modifier.height(4.dp))
                Bullets(
                    listOf(
                        "Crear y gestionar su cuenta de usuario dentro de la aplicación.",
                        "Procesar, asignar y rastrear pedidos de garrafones de agua a domicilio.",
                        "Notificarle sobre el estado de su pedido y la proximidad del repartidor.",
                        "Calcular la ruta óptima de entrega para el repartidor.",
                        "Mantener un historial de pedidos para consulta del propio usuario.",
                        "Prevenir fraudes mediante verificación OTP y penalización por ausencias reiteradas."
                    )
                )
            }

            Seccion(titulo = "4. Transferencias de datos") {
                Parrafo(
                    "H2ToGo no vende, cede ni comercializa sus datos personales a terceros con fines de " +
                        "mercadotecnia. Los únicos casos de transferencia son:"
                )
                Spacer(Modifier.height(4.dp))
                BulletsAnotados(
                    listOf(
                        "Infraestructura en la nube:" to " los datos se alojan en servidores de Microsoft " +
                            "Azure, quien actúa como encargado del tratamiento bajo acuerdo de " +
                            "confidencialidad. Azure no utiliza sus datos para fines propios.",
                        "Entre actores del servicio:" to " la ubicación GPS del repartidor es visible para " +
                            "el cliente cuyo pedido está activo, y el domicilio del cliente es visible " +
                            "para el repartidor asignado. Esta transferencia es estrictamente necesaria " +
                            "para la operación del servicio."
                    )
                )
            }

            Seccion(titulo = "5. Derechos ARCO") {
                Parrafo(
                    "Usted tiene derecho a Acceder, Rectificar, Cancelar u Oponerse al tratamiento de " +
                        "sus datos personales (derechos ARCO), conforme a los Arts. 22–25 de la LFPDPPP. " +
                        "Para ejercerlos:"
                )
                Spacer(Modifier.height(4.dp))
                Bullets(
                    listOf(
                        "Envíe una solicitud al correo de contacto (gmarquezr1900@alumno.ipn.mx o btrejoh1900@alumno.ipn.mx).",
                        "Indique el derecho que desea ejercer y los datos a los que se refiere.",
                        "Recibirá una respuesta en un plazo máximo de 20 días hábiles."
                    )
                )
                Spacer(Modifier.height(6.dp))
                ParrafoAnotado(
                    bold = "Nota:",
                    resto = " el historial de pedidos se conserva de forma inmutable por integridad " +
                        "operativa y no puede ser eliminado individualmente. Esta limitación se informa " +
                        "expresamente al momento de la aceptación de este aviso."
                )
            }

            Seccion(titulo = "6. Revocación del consentimiento") {
                Parrafo(
                    "Usted puede revocar su consentimiento para el tratamiento de sus datos en cualquier " +
                        "momento, salvo en los casos en que el tratamiento sea necesario para el " +
                        "cumplimiento de una obligación legal. La revocación implica la desactivación de " +
                        "su cuenta y la imposibilidad de utilizar el servicio. Para solicitarla, utilice " +
                        "el mismo canal indicado en la sección de Derechos ARCO."
                )
            }

            Seccion(titulo = "7. Medidas de seguridad") {
                Parrafo("H2ToGo implementa las siguientes medidas técnicas para proteger sus datos:")
                Spacer(Modifier.height(4.dp))
                Bullets(
                    listOf(
                        "Cifrado bcrypt de contraseñas; nunca se almacenan en texto plano.",
                        "Transmisión de datos bajo protocolo HTTPS/TLS.",
                        "Control de acceso por roles: cada usuario solo puede ver los datos que le corresponden.",
                        "Sesión única activa por cuenta para prevenir suplantaciones.",
                        "Registro inmutable de cambios de estado con marca de tiempo."
                    )
                )
            }

            Seccion(titulo = "8. Cambios a este Aviso de Privacidad") {
                Parrafo(
                    "Cualquier modificación a este aviso será notificada a través de la aplicación. El " +
                        "uso continuado del servicio tras la notificación implica la aceptación de los " +
                        "cambios."
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = HToGoColors.OutlineSoft, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Boceto académico — IPN ESCOM 2025. Sujeto a revisión legal antes de producción.",
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                color = HToGoColors.TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DocHeader() {
    Column {
        Text(
            "AVISO DE PRIVACIDAD",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HToGoColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Boceto para prototipo académico — sujeto a revisión legal antes de producción",
            fontSize = 12.sp,
            color = HToGoColors.TextSecondary,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun Seccion(titulo: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(
            titulo,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HToGoColors.PrimaryDark
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun Parrafo(texto: String) {
    Text(
        texto,
        fontSize = 13.sp,
        color = HToGoColors.TextSecondary,
        lineHeight = 21.sp
    )
}

@Composable
private fun ParrafoAnotado(bold: String, resto: String) {
    val txt = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)) {
            append(bold)
        }
        append(resto)
    }
    Text(
        txt,
        fontSize = 13.sp,
        color = HToGoColors.TextSecondary,
        lineHeight = 21.sp
    )
}

@Composable
private fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { Bullet(it) }
    }
}

@Composable
private fun Bullet(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "·",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HToGoColors.Primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            texto,
            fontSize = 13.sp,
            color = HToGoColors.TextSecondary,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun BulletsAnotados(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (bold, resto) ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "·",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.Primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                val txt = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HToGoColors.TextPrimary)) {
                        append(bold)
                    }
                    append(resto)
                }
                Text(
                    txt,
                    fontSize = 13.sp,
                    color = HToGoColors.TextSecondary,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun CardDatoSensible(titulo: String, texto: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HToGoColors.PrimaryWash,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    brush = SolidColor(HToGoColors.Primary),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3.dp.toPx() * 2
                )
            }
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn, null,
                    tint = HToGoColors.Primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    titulo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HToGoColors.PrimaryDark
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                texto,
                fontSize = 12.sp,
                color = HToGoColors.TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun AvisoPrivacidadScreenPreview() {
    HToGoTheme { AvisoPrivacidadScreen() }
}
