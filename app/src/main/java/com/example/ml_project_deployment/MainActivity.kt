package com.example.ml_project_deployment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --- Retrofit API Interface ---
interface MyApi {
    @POST("predict/")
    fun postPrediction(@Body input: CovidInput): Call<String>  // Response is plain string
}

// --- Data Classes ---
data class CovidInput(
    val Breathing_Problem: Float,
    val Fever: Float,
    val Dry_Cough: Float,
    val Sore_Throat: Float,
    val Running_Nose: Float,
    val Asthma: Float,
    val Chronic_Lung_Disease: Float,
    val Headache: Float,
    val Heart_Disease: Float,
    val Diabetes: Float,
    val Hyper_Tension: Float,
    val Fatigue: Float,
    val Gastrointestinal: Float,
    val Abroad_Travel: Float,
    val Contact_with_COVID_Patient: Float,
    val Attended_Large_Gathering: Float,
    val Visited_Public_Exposed_Places: Float,
    val Family_Working_in_Public_Exposed_Places: Float,
    val Wearing_Masks: Float,
    val Sanitization_from_Market: Float
)

// --- Main Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Logging interceptor for debugging network calls
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://kiyoya123-covid19.hf.space/")  // Keep the trailing slash in baseUrl
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())  // Add this for string responses
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(MyApi::class.java)

        setContent {
            MaterialTheme {
                SymptomInputScreen(api)
            }
        }
    }
}

// --- Compose UI ---
@Composable
fun SymptomInputScreen(api: MyApi) {
    var responses by remember { mutableStateOf<List<Float>>(emptyList()) }
    var predictionResult by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val symptomLabels = listOf(
        "Breathing Problem", "Fever", "Dry Cough", "Sore Throat", "Running Nose",
        "Asthma", "Chronic Lung Disease", "Headache", "Heart Disease", "Diabetes",
        "Hyper Tension", "Fatigue", "Gastrointestinal", "Abroad Travel",
        "Contact with COVID Patient", "Attended Large Gathering",
        "Visited Public Exposed Places", "Family Working in Public Exposed Places",
        "Wearing Masks", "Sanitization from Market"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("COVID-19 Risk Assessment", style = MaterialTheme.typography.headlineMedium)
        Text("Answer Yes or No for each question:", style = MaterialTheme.typography.titleMedium)

        if (responses.size < 20) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Question ${responses.size + 1} of 20",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        symptomLabels[responses.size],
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { responses = responses + 1f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yes")
                        }
                        Button(
                            onClick = { responses = responses + 0f },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No")
                        }
                    }
                }
            }
        }

        LinearProgressIndicator(
            progress = responses.size / 20f,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Progress: ${responses.size} / 20", style = MaterialTheme.typography.bodyMedium)

        if (responses.size == 20 && !submitted) {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = ""

                    val input = CovidInput(
                        Breathing_Problem = responses[0],
                        Fever = responses[1],
                        Dry_Cough = responses[2],
                        Sore_Throat = responses[3],
                        Running_Nose = responses[4],
                        Asthma = responses[5],
                        Chronic_Lung_Disease = responses[6],
                        Headache = responses[7],
                        Heart_Disease = responses[8],
                        Diabetes = responses[9],
                        Hyper_Tension = responses[10],
                        Fatigue = responses[11],
                        Gastrointestinal = responses[12],
                        Abroad_Travel = responses[13],
                        Contact_with_COVID_Patient = responses[14],
                        Attended_Large_Gathering = responses[15],
                        Visited_Public_Exposed_Places = responses[16],
                        Family_Working_in_Public_Exposed_Places = responses[17],
                        Wearing_Masks = responses[18],
                        Sanitization_from_Market = responses[19]
                    )

                    api.postPrediction(input).enqueue(object : Callback<String> {
                        override fun onResponse(
                            call: Call<String>,
                            response: Response<String>
                        ) {
                            isLoading = false
                            if (response.isSuccessful) {
                                predictionResult = response.body() ?: "No result"
                            } else {
                                errorMessage = "Error: ${response.code()} - ${response.message()}"
                            }
                            submitted = true
                        }

                        override fun onFailure(call: Call<String>, t: Throwable) {
                            isLoading = false
                            errorMessage = "Network error: ${t.message}"
                            submitted = true
                        }
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Answers")
                }
            }
        }

        if (submitted) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (errorMessage.isEmpty())
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "Risk Assessment Result",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            predictionResult,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    responses = emptyList()
                    predictionResult = ""
                    errorMessage = ""
                    submitted = false
                    isLoading = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Over")
            }
        }

        if (responses.isNotEmpty() && !submitted) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    responses = emptyList()
                    predictionResult = ""
                    errorMessage = ""
                    submitted = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Answers")
            }
        }
    }
}