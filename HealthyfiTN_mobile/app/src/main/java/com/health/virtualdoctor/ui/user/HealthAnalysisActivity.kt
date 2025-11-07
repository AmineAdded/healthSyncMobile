package com.health.virtualdoctor.ui.user

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.health.virtualdoctor.R
import com.health.virtualdoctor.databinding.ActivityHealthAnalysisBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HealthAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHealthAnalysisBinding
    private lateinit var anomaliesAdapter: AnomaliesAdapter
    private lateinit var recommendationsAdapter: RecommendationsAdapter

    companion object {
        const val EXTRA_USER_DATA = "user_data_json"
        const val AI_API_URL = "https://poster-meals-russell-metallica.trycloudflare.com/analyze-health" // Changez l'IP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        // Récupérer les données envoyées depuis UserMetricsActivity
        val userDataJson = intent.getStringExtra(EXTRA_USER_DATA)

        if (userDataJson != null) {
            analyzeHealth(userDataJson)
        } else {
            Toast.makeText(this, "Erreur: Aucune donnée à analyser", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // RecyclerView pour anomalies
        anomaliesAdapter = AnomaliesAdapter()
        binding.rvAnomalies.apply {
            layoutManager = LinearLayoutManager(this@HealthAnalysisActivity)
            adapter = anomaliesAdapter
        }

        // RecyclerView pour recommandations
        recommendationsAdapter = RecommendationsAdapter()
        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(this@HealthAnalysisActivity)
            adapter = recommendationsAdapter
        }
    }

    private fun analyzeHealth(userDataJson: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.contentLayout.visibility = android.view.View.GONE

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val requestBody = userDataJson.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(AI_API_URL)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""

                    response.close()
                    responseBody
                }

                // Parser la réponse
                val jsonResponse = JSONObject(result)
                displayResults(jsonResponse)

            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@HealthAnalysisActivity,
                    "Erreur d'analyse: ${e.message}",
                    Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun displayResults(json: JSONObject) {
        binding.progressBar.visibility = android.view.View.GONE
        binding.contentLayout.visibility = android.view.View.VISIBLE

        // Score de santé
        val healthScore = json.getDouble("healthScore")
        binding.tvHealthScore.text = String.format("%.1f", healthScore)

        // Niveau de risque
        val riskLevel = json.getString("riskLevel")
        binding.tvRiskLevel.text = riskLevel
        binding.tvRiskLevel.setTextColor(getRiskColor(riskLevel))

        // Icône et couleur du score
        binding.scoreCircle.setBackgroundResource(getScoreBackground(healthScore))

        // Détails du score
        val insights = json.getJSONObject("insights")
        val breakdown = insights.getJSONObject("score_breakdown")

        binding.tvActivityScore.text = String.format("%.1f/25", breakdown.getDouble("activity"))
        binding.tvCardioScore.text = String.format("%.1f/25", breakdown.getDouble("cardiovascular"))
        binding.tvSleepScore.text = String.format("%.1f/20", breakdown.getDouble("sleep"))
        binding.tvHydrationScore.text = String.format("%.1f/10", breakdown.getDouble("hydration"))
        binding.tvStressScore.text = String.format("%.1f/10", breakdown.getDouble("stress"))
        binding.tvVitalsScore.text = String.format("%.1f/10", breakdown.getDouble("vitals"))

        // Progress bars
        binding.pbActivity.progress = (breakdown.getDouble("activity") / 25 * 100).toInt()
        binding.pbCardio.progress = (breakdown.getDouble("cardiovascular") / 25 * 100).toInt()
        binding.pbSleep.progress = (breakdown.getDouble("sleep") / 20 * 100).toInt()
        binding.pbHydration.progress = (breakdown.getDouble("hydration") / 10 * 100).toInt()
        binding.pbStress.progress = (breakdown.getDouble("stress") / 10 * 100).toInt()
        binding.pbVitals.progress = (breakdown.getDouble("vitals") / 10 * 100).toInt()

        // Explication IA
        binding.tvAiExplanation.text = json.getString("aiExplanation")

        // Anomalies
        val anomaliesArray = json.getJSONArray("anomalies")
        val anomaliesList = mutableListOf<String>()
        for (i in 0 until anomaliesArray.length()) {
            anomaliesList.add(anomaliesArray.getString(i))
        }
        anomaliesAdapter.submitList(anomaliesList)
        binding.tvAnomaliesCount.text = "${anomaliesList.size} anomalie(s) détectée(s)"

        // Recommandations
        val recommendationsArray = json.getJSONArray("recommendations")
        val recommendationsList = mutableListOf<String>()
        for (i in 0 until recommendationsArray.length()) {
            recommendationsList.add(recommendationsArray.getString(i))
        }
        recommendationsAdapter.submitList(recommendationsList)
    }

    private fun getRiskColor(risk: String): Int {
        return when (risk) {
            "Faible" -> getColor(R.color.green_500)
            "Modéré" -> getColor(R.color.yellow_500)
            "Élevé" -> getColor(R.color.orange_500)
            "Critique" -> getColor(R.color.red_500)
            else -> getColor(R.color.gray_500)
        }
    }

    private fun getScoreBackground(score: Double): Int {
        return when {
            score >= 80 -> R.drawable.bg_score_excellent
            score >= 60 -> R.drawable.bg_score_good
            score >= 40 -> R.drawable.bg_score_moderate
            else -> R.drawable.bg_score_poor
        }
    }
}

// Adapter pour les anomalies
class AnomaliesAdapter : RecyclerView.Adapter<AnomaliesAdapter.ViewHolder>() {

    private var items = listOf<String>()

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anomaly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvAnomaly: android.widget.TextView = itemView.findViewById(R.id.tvAnomaly)

        fun bind(anomaly: String) {
            tvAnomaly.text = anomaly

            // Couleur selon gravité
            when {
                anomaly.contains("🚨") -> tvAnomaly.setTextColor(itemView.context.getColor(R.color.red_500))
                anomaly.contains("⚠️") -> tvAnomaly.setTextColor(itemView.context.getColor(R.color.orange_500))
                else -> tvAnomaly.setTextColor(itemView.context.getColor(R.color.gray_700))
            }
        }
    }
}

// Adapter pour les recommandations
class RecommendationsAdapter : RecyclerView.Adapter<RecommendationsAdapter.ViewHolder>() {

    private var items = listOf<String>()

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvRecommendation: android.widget.TextView = itemView.findViewById(R.id.tvRecommendation)

        fun bind(recommendation: String) {
            tvRecommendation.text = recommendation
        }
    }
}