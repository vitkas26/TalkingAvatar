package kg.nurtelecom.o.talkingavatar.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import kg.nurtelecom.o.talkingavatar.data.speech.EngineSettings
import kg.nurtelecom.o.talkingavatar.data.speech.SttEngineChoice
import kg.nurtelecom.o.talkingavatar.data.speech.TtsEngineChoice
import kg.nurtelecom.o.talkingavatar.domain.model.Language
import kg.nurtelecom.o.talkingavatar.ui.conversation.MainViewModel
import kg.nurtelecom.o.talkingavatar.ui.utils.rotateFullScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private fun sttChoiceLabel(choice: SttEngineChoice) = when (choice) {
    SttEngineChoice.AUTO -> "Авто (по языку: ky → AkylAI, остальное → Whisper)"
    SttEngineChoice.SYSTEM -> "Системный Android (SpeechRecognizer)"
    SttEngineChoice.WHISPER -> "Whisper (cloud)"
    SttEngineChoice.AKYLAI -> "AkylAI-STT"
    SttEngineChoice.GOOGLE_CLOUD -> "Google Cloud STT"
    SttEngineChoice.GOOGLE_CLOUD_PLUS_AKYLAI -> "Google Cloud + AkylAI (по языку: ky → AkylAI, остальное → Google Cloud)"
}

private fun ttsChoiceLabel(choice: TtsEngineChoice) = when (choice) {
    TtsEngineChoice.AUTO -> "Авто (по языку: ky → AkylAI, остальное → системный)"
    TtsEngineChoice.SYSTEM -> "Системный Android TTS"
    TtsEngineChoice.AKYLAI -> "AkylAI-TTS-mini"
    TtsEngineChoice.PIPER -> "Piper TTS"
    TtsEngineChoice.PIPER_AKYLAI -> "Piper + AkylAI (по языку: ky → AkylAI, остальное → Piper)"
    TtsEngineChoice.GOOGLE_CLOUD -> "Google Cloud TTS (ky не поддерживается)"
    TtsEngineChoice.GOOGLE_CLOUD_PLUS_AKYLAI -> "Google Cloud + AkylAI (по языку: ky → AkylAI, остальное → Google Cloud)"
}

// Debug-экран для пилота: выбор языка и ручной оверрайд STT/TTS-движка для тестирования
// разных комбинаций на демо. Не продовый UI — минимально оформлен намеренно.
@Composable
fun SettingsScreen(onStart: () -> Unit) {
    val mainViewModel = koinViewModel<MainViewModel>()
    val engineSettings = koinInject<EngineSettings>()

    var selectedLanguage by remember { mutableStateOf(Language.Russian) }
    var sttChoice by remember { mutableStateOf(engineSettings.sttChoice) }
    var ttsChoice by remember { mutableStateOf(engineSettings.ttsChoice) }
    var akylAiBaseUrl by remember { mutableStateOf(engineSettings.akylAiBaseUrl) }
    var whisperBaseUrl by remember { mutableStateOf(engineSettings.whisperBaseUrl) }
    var piperBaseUrl by remember { mutableStateOf(engineSettings.piperBaseUrl) }
    var googleCloudBaseUrl by remember { mutableStateOf(engineSettings.googleCloudBaseUrl) }
    var googleCloudProxyToken by remember { mutableStateOf(engineSettings.googleCloudProxyToken) }
    var googleCloudAltLanguages by remember { mutableStateOf(engineSettings.googleCloudAltLanguages) }
    var googleCloudApiVersion by remember { mutableStateOf(engineSettings.googleCloudApiVersion) }
    var googleCloudTtsTier by remember { mutableStateOf(engineSettings.googleCloudTtsTier) }
    var vadSilenceThresholdDb by remember { mutableStateOf(engineSettings.vadSilenceThresholdDb.toString()) }
    var vadSilenceDurationMs by remember { mutableStateOf(engineSettings.vadSilenceDurationMs.toString()) }
    var vadMaxRecordingMs by remember { mutableStateOf(engineSettings.vadMaxRecordingMs.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Настройки (debug, пилот)", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(24.dp))
        Text("Язык", style = MaterialTheme.typography.titleMedium)
        Language.entries.forEach { language ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selectedLanguage == language, onClick = { selectedLanguage = language })
                Text(language.displayName)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("STT-движок", style = MaterialTheme.typography.titleMedium)
        SttEngineChoice.entries.forEach { choice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = sttChoice == choice, onClick = { sttChoice = choice })
                Text(sttChoiceLabel(choice))
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("TTS-движок", style = MaterialTheme.typography.titleMedium)
        TtsEngineChoice.entries.forEach { choice ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = ttsChoice == choice, onClick = { ttsChoice = choice })
                Text(ttsChoiceLabel(choice))
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Адрес AkylAI-сервиса", style = MaterialTheme.typography.titleMedium)
        Text(
            "Эмулятор: http://10.0.2.2:8000/. Реальное устройство: http://<IP ноутбука в Wi-Fi>:8000/",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = akylAiBaseUrl,
            onValueChange = { akylAiBaseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("Адрес Whisper-сервиса", style = MaterialTheme.typography.titleMedium)
        Text(
            "Эмулятор: http://10.0.2.2:8001/. Реальное устройство: http://<IP ноутбука в Wi-Fi>:8001/",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = whisperBaseUrl,
            onValueChange = { whisperBaseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("Адрес Piper-сервиса", style = MaterialTheme.typography.titleMedium)
        Text(
            "Эмулятор: http://10.0.2.2:8002/. Реальное устройство: http://<IP ноутбука в Wi-Fi>:8002/",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = piperBaseUrl,
            onValueChange = { piperBaseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("Адрес Google Cloud proxy", style = MaterialTheme.typography.titleMedium)
        Text(
            "https://138.16.155.105/google-cloud-proxy/ (или свой хост для теста)",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = googleCloudBaseUrl,
            onValueChange = { googleCloudBaseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("X-Proxy-Token (google-cloud-proxy)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = googleCloudProxyToken,
            onValueChange = { googleCloudProxyToken = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("Google Cloud STT: alt-языки (через запятую)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Пусто — берётся дефолт по основному языку (см. GoogleCloudSttEngine.defaultAltLanguagesByPrimary)",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = googleCloudAltLanguages,
            onValueChange = { googleCloudAltLanguages = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(16.dp))
        Text("Google Cloud STT: версия API (A/B, временно)", style = MaterialTheme.typography.titleMedium)
        Text(
            "v2 игнорирует lang/alt — полный автодетект языка на стороне Google",
            style = MaterialTheme.typography.bodySmall,
        )
        listOf("v1", "v2").forEach { version ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = googleCloudApiVersion == version,
                    onClick = { googleCloudApiVersion = version },
                )
                Text(version)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Google Cloud TTS: голос (A/B демо, временно)", style = MaterialTheme.typography.titleMedium)
        Text(
            "wavenet не покрывает ky-KG (там всегда AkylAI)",
            style = MaterialTheme.typography.bodySmall,
        )
        listOf("chirp3hd" to "Chirp3-HD (дефолт)", "wavenet" to "WaveNet").forEach { (tier, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = googleCloudTtsTier == tier,
                    onClick = { googleCloudTtsTier = tier },
                )
                Text(label)
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("VAD: конец фразы по паузе (debug)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Порог в дБ не откалиброван под шумный ТРЦ — тюнить здесь. Общий для Whisper/AkylAI/Google Cloud STT",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Порог тишины, дБFS (отрицательное число, напр. -40)", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = vadSilenceThresholdDb,
            onValueChange = { vadSilenceThresholdDb = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Text("Длительность тишины до остановки, мс (бизнес-треб. 3000-5000)", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = vadSilenceDurationMs,
            onValueChange = { vadSilenceDurationMs = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Text("Максимальная длительность записи, мс (safety net)", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = vadMaxRecordingMs,
            onValueChange = { vadMaxRecordingMs = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                engineSettings.sttChoice = sttChoice
                engineSettings.ttsChoice = ttsChoice
                engineSettings.akylAiBaseUrl = akylAiBaseUrl
                    .trim()
                    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                    .let { if (it.endsWith("/")) it else "$it/" }
                engineSettings.whisperBaseUrl = whisperBaseUrl
                    .trim()
                    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                    .let { if (it.endsWith("/")) it else "$it/" }
                engineSettings.piperBaseUrl = piperBaseUrl
                    .trim()
                    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                    .let { if (it.endsWith("/")) it else "$it/" }
                engineSettings.googleCloudBaseUrl = googleCloudBaseUrl
                    .trim()
                    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                    .let { if (it.endsWith("/")) it else "$it/" }
                engineSettings.googleCloudProxyToken = googleCloudProxyToken.trim()
                engineSettings.googleCloudAltLanguages = googleCloudAltLanguages.trim()
                engineSettings.googleCloudApiVersion = googleCloudApiVersion
                engineSettings.googleCloudTtsTier = googleCloudTtsTier
                engineSettings.vadSilenceThresholdDb = vadSilenceThresholdDb.trim().toDoubleOrNull()
                    ?: engineSettings.vadSilenceThresholdDb
                engineSettings.vadSilenceDurationMs = vadSilenceDurationMs.trim().toLongOrNull()
                    ?: engineSettings.vadSilenceDurationMs
                engineSettings.vadMaxRecordingMs = vadMaxRecordingMs.trim().toLongOrNull()
                    ?: engineSettings.vadMaxRecordingMs
                mainViewModel.setInitialLanguage(selectedLanguage)
                onStart()
            },
        ) {
            Text("Начать")
        }
    }
}
