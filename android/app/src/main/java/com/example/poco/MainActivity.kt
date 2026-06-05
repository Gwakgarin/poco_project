package com.example.poco

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class SoundEventResponse(
    val id: Long,
    val rawFile: String,
    val splitFile: String,
    val predLabel: String,
    val predScore: Double,
    val segIndex: Int,
    val startSec: Int,
    val endSec: Int,
    val smoothedLabel: String
)

interface ApiService {
    @GET("/api/sound-events")
    suspend fun getSoundEvents(): List<SoundEventResponse>
}

object RetrofitClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 마이크 권한 요청
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            0
        )

        // 데시벨 트리거 시작
        Thread {
            AudioTrigger().startListening()
        }.start()

        setContent {
            PocoScreen()
        }
    }
}

@Composable
fun PocoScreen() {

    var eventList by remember {
        mutableStateOf(listOf<SoundEventResponse>())
    }

    var serverStatus by remember {
        mutableStateOf("대기 중")
    }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "POCO 서버 연결 테스트",
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "서버 상태",
            fontSize = 20.sp
        )

        Text(
            text = serverStatus,
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "조회된 소리 이벤트",
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (eventList.isEmpty()) {

            Text("아직 조회된 이벤트 없음")

        } else {

            eventList.forEachIndexed { index, event ->

                Text(
                    text =
                    "${index + 1}. " +
                    "${event.predLabel} / " +
                    "${event.predScore} / " +
                    "${event.startSec}~${event.endSec}초",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {

                scope.launch {

                    serverStatus = "서버 조회 중..."

                    try {

                        val result =
                            RetrofitClient.api.getSoundEvents()

                        eventList = result

                        serverStatus = "서버 조회 성공"

                    } catch (e: Exception) {

                        serverStatus =
                            "서버 조회 실패: ${e.message}"
                    }
                }
            }
        ) {
            Text("서버 소리 이벤트 조회")
        }
    }
}
