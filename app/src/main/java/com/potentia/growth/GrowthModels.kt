package com.potentia.growth

enum class GrowthExerciseMode {
    GUIDED_REFLECTION,
    INTERACTIVE_CHALLENGE
}

data class GrowthChoice(
    val value: String,
    val label: String
)

data class GrowthChallenge(
    val prompt: String,
    val choices: List<GrowthChoice>,
    val correctValue: String,
    val successExplanation: String,
    val retryHint: String
)

data class GrowthReflectionPrompt(
    val id: String,
    val label: String,
    val placeholder: String,
    val minChars: Int = 3
)

data class GrowthExercise(
    val id: String,
    val dimensionId: String,
    val title: String,
    val summary: String,
    val estimatedMinutes: Int = 10,
    val steps: List<String>,
    val mode: GrowthExerciseMode,
    val reflectionPrompts: List<GrowthReflectionPrompt> = emptyList(),
    val challenge: GrowthChallenge? = null
)

data class GrowthPracticeRecord(
    val exerciseId: String,
    val responses: Map<String, String>,
    val verifiedInteraction: Boolean,
    val completedAt: Long
)

data class GrowthProgress(
    val weekKey: String,
    val dimensionId: String,
    val selectedExerciseId: String? = null,
    val activeExerciseId: String? = null,
    val completedExerciseIds: Set<String> = emptySet(),
    val practiceRecords: Map<String, GrowthPracticeRecord> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
)

object GrowthCatalog {
    private fun reflection(
        id: String,
        dimensionId: String,
        title: String,
        summary: String,
        steps: List<String>,
        prompts: List<GrowthReflectionPrompt>,
        estimatedMinutes: Int = 10
    ) = GrowthExercise(
        id = id,
        dimensionId = dimensionId,
        title = title,
        summary = summary,
        estimatedMinutes = estimatedMinutes,
        steps = steps,
        mode = GrowthExerciseMode.GUIDED_REFLECTION,
        reflectionPrompts = prompts
    )

    private fun challenge(
        id: String,
        dimensionId: String,
        title: String,
        summary: String,
        steps: List<String>,
        challenge: GrowthChallenge,
        estimatedMinutes: Int = 7
    ) = GrowthExercise(
        id = id,
        dimensionId = dimensionId,
        title = title,
        summary = summary,
        estimatedMinutes = estimatedMinutes,
        steps = steps,
        mode = GrowthExerciseMode.INTERACTIVE_CHALLENGE,
        challenge = challenge
    )

    private val exercises = listOf(
        challenge(
            id = "logical_deduction",
            dimensionId = "logical",
            title = "Deduksi 3 Petunjuk",
            summary = "Latih membedakan kesimpulan yang benar-benar mengikuti informasi dari dugaan.",
            steps = listOf(
                "Baca premis tanpa menambah asumsi baru.",
                "Cari informasi yang berlaku untuk semua anggota kelompok.",
                "Pilih kesimpulan yang pasti mengikuti premis."
            ),
            challenge = GrowthChallenge(
                prompt = "Semua anggota Tim A sudah menyelesaikan modul dasar. Rina adalah anggota Tim A. Kesimpulan mana yang pasti benar?",
                choices = listOf(
                    GrowthChoice("a", "Rina pasti sudah menyelesaikan modul dasar."),
                    GrowthChoice("b", "Rina mungkin belum menyelesaikan modul dasar."),
                    GrowthChoice("c", "Rina adalah anggota paling cepat di Tim A."),
                    GrowthChoice("d", "Semua yang menyelesaikan modul dasar adalah anggota Tim A.")
                ),
                correctValue = "a",
                successExplanation = "Premis berlaku untuk semua anggota Tim A. Karena Rina anggota Tim A, kesimpulan bahwa ia sudah menyelesaikan modul dasar mengikuti premis secara langsung.",
                retryHint = "Perhatikan mana informasi yang benar-benar dinyatakan untuk semua anggota Tim A. Jangan menambahkan informasi tentang kecepatan atau hubungan sebaliknya."
            )
        ),
        reflection(
            id = "logical_argument",
            dimensionId = "logical",
            title = "Susun Alur Argumen",
            summary = "Amati bagaimana kamu memisahkan klaim, alasan, bukti, dan bantahan.",
            steps = listOf(
                "Pilih satu pendapat atau keputusan yang sedang kamu pertimbangkan.",
                "Tuliskan alasan utama dan bukti/contoh yang benar-benar mendukungnya.",
                "Cari satu kemungkinan bantahan lalu periksa apakah kesimpulanmu perlu diubah."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("claim", "Pendapat atau keputusan", "Apa yang sedang kamu pertimbangkan?"),
                GrowthReflectionPrompt("reason", "Alasan + bukti", "Tuliskan alasan dan bukti yang benar-benar kamu punya.", 8),
                GrowthReflectionPrompt("insight", "Apa yang kamu sadari?", "Apakah ada asumsi, lompatan logika, atau alasan yang ternyata lemah?", 8)
            )
        ),
        reflection(
            id = "logical_strategy",
            dimensionId = "logical",
            title = "Rencana Tiga Langkah",
            summary = "Amati cara kamu memikirkan urutan tindakan dan konsekuensi.",
            steps = listOf(
                "Pilih satu target kecil yang realistis hari ini.",
                "Susun tiga langkah berurutan menuju target tersebut.",
                "Periksa satu hambatan paling mungkin dan siapkan alternatif."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("target", "Target kecil", "Target apa yang ingin kamu capai?"),
                GrowthReflectionPrompt("sequence", "Tiga langkah", "Tuliskan langkah 1 → 2 → 3.", 8),
                GrowthReflectionPrompt("fallback", "Hambatan dan alternatif", "Hambatan apa yang paling mungkin, dan apa rencana cadanganmu?", 8)
            )
        ),

        reflection(
            id = "creative_alternative_uses",
            dimensionId = "creative",
            title = "5 Penggunaan Alternatif",
            summary = "Latih menghasilkan banyak kemungkinan sebelum menilai ide pertama.",
            steps = listOf(
                "Pilih satu benda sederhana di dekatmu.",
                "Tuliskan minimal lima penggunaan lain selain fungsi utamanya.",
                "Pilih satu ide yang paling tidak biasa tetapi masih masuk akal."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("object", "Benda yang dipilih", "Contoh: botol, kardus, sendok."),
                GrowthReflectionPrompt("ideas", "Lima penggunaan alternatif", "Tulis minimal lima ide, pisahkan dengan baris atau koma.", 20),
                GrowthReflectionPrompt("insight", "Ide paling menarik dan alasannya", "Ide mana yang paling tidak biasa tetapi masih relevan? Mengapa?", 8)
            )
        ),
        reflection(
            id = "creative_three_solutions",
            dimensionId = "creative",
            title = "Tiga Solusi Berbeda",
            summary = "Amati apakah kamu berhenti di solusi pertama atau mampu mengganti sudut pendekatan.",
            steps = listOf(
                "Pilih satu masalah kecil yang nyata.",
                "Buat tiga solusi yang berbeda pendekatannya.",
                "Bandingkan dari sisi kemudahan, waktu/biaya, dan kebaruan."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("problem", "Masalah yang dipilih", "Masalah kecil apa yang ingin kamu selesaikan?"),
                GrowthReflectionPrompt("solutions", "Tiga solusi berbeda", "Tulis solusi 1, 2, dan 3.", 15),
                GrowthReflectionPrompt("insight", "Apa yang kamu sadari?", "Solusi mana yang awalnya tidak terpikirkan? Apa yang membantumu menemukannya?", 8)
            )
        ),
        reflection(
            id = "creative_combine",
            dimensionId = "creative",
            title = "Gabungkan Dua Ide",
            summary = "Latih menghubungkan dua konsep yang awalnya tidak berkaitan.",
            steps = listOf(
                "Pilih dua benda, aplikasi, atau aktivitas yang berbeda.",
                "Bayangkan satu produk/kebiasaan baru yang menggabungkan keduanya.",
                "Tulis satu manfaat dan satu keterbatasannya."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("pair", "Dua hal yang digabungkan", "Contoh: kalender + permainan."),
                GrowthReflectionPrompt("combination", "Hasil kombinasi", "Apa ide baru yang muncul dari keduanya?", 8),
                GrowthReflectionPrompt("tradeoff", "Manfaat dan keterbatasan", "Apa manfaat utama dan kelemahan ide tersebut?", 8)
            )
        ),

        reflection(
            id = "verbal_summary",
            dimensionId = "verbal",
            title = "Ringkas Jadi 3 Kalimat",
            summary = "Amati kemampuanmu menangkap ide inti tanpa mempertahankan semua detail.",
            steps = listOf(
                "Pilih bacaan pendek yang sudah kamu miliki.",
                "Tulis satu kalimat untuk ide utama dan dua kalimat untuk detail terpenting.",
                "Baca ulang lalu hapus kata yang tidak menambah makna."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("source", "Topik bacaan", "Bacaan tentang apa?"),
                GrowthReflectionPrompt("summary", "Ringkasan 3 kalimat", "Tulis tepat tiga kalimat inti.", 20),
                GrowthReflectionPrompt("insight", "Bagian tersulit", "Apa yang paling sulit: memilih ide utama, membuang detail, atau merangkai kalimat?", 8)
            )
        ),
        challenge(
            id = "verbal_word_relation",
            dimensionId = "verbal",
            title = "Relasi Kata",
            summary = "Latih mengenali hubungan makna secara eksplisit.",
            steps = listOf(
                "Identifikasi hubungan pada pasangan kata pertama.",
                "Cari pilihan yang memiliki hubungan paling sejenis.",
                "Jangan hanya mencari kata yang terasa berhubungan."
            ),
            challenge = GrowthChallenge(
                prompt = "Buku : Membaca = Musik : ...",
                choices = listOf(
                    GrowthChoice("a", "Menulis"),
                    GrowthChoice("b", "Mendengar"),
                    GrowthChoice("c", "Menggambar"),
                    GrowthChoice("d", "Berjalan")
                ),
                correctValue = "b",
                successExplanation = "Buku umumnya diproses melalui aktivitas membaca, sedangkan musik melalui aktivitas mendengar. Hubungannya adalah media → aktivitas utama untuk mengaksesnya.",
                retryHint = "Cari hubungan fungsi/aktivitas pada pasangan pertama, lalu terapkan hubungan yang sama ke 'Musik'."
            )
        ),
        reflection(
            id = "verbal_explain_simple",
            dimensionId = "verbal",
            title = "Jelaskan dengan Bahasa Sederhana",
            summary = "Amati apakah penjelasanmu benar-benar bisa dipahami tanpa istilah yang tidak dijelaskan.",
            steps = listOf(
                "Pilih satu konsep yang kamu pelajari minggu ini.",
                "Jelaskan seolah-olah kepada orang yang belum pernah mempelajarinya.",
                "Batasi maksimal lima kalimat dan jelaskan istilah teknis."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("concept", "Konsep", "Konsep apa yang ingin kamu jelaskan?"),
                GrowthReflectionPrompt("explanation", "Penjelasan sederhana", "Jelaskan maksimal lima kalimat.", 20),
                GrowthReflectionPrompt("insight", "Apa yang kamu ubah?", "Istilah atau bagian mana yang harus kamu sederhanakan?", 8)
            )
        ),

        challenge(
            id = "spatial_rotation",
            dimensionId = "spatial",
            title = "Rotasi Mental Singkat",
            summary = "Latih memperbarui orientasi objek setelah diputar.",
            steps = listOf(
                "Bayangkan arah awal objek.",
                "Terapkan rotasi tanpa membalik seperti cermin.",
                "Periksa arah akhir."
            ),
            challenge = GrowthChallenge(
                prompt = "Sebuah panah mengarah ke atas (↑). Jika diputar 90° searah jarum jam, ke mana arah akhirnya?",
                choices = listOf(
                    GrowthChoice("a", "←"),
                    GrowthChoice("b", "↑"),
                    GrowthChoice("c", "→"),
                    GrowthChoice("d", "↓")
                ),
                correctValue = "c",
                successExplanation = "Rotasi 90° searah jarum jam mengubah arah atas menjadi kanan.",
                retryHint = "Bayangkan jarum jam bergerak dari posisi 12 ke posisi 3."
            )
        ),
        reflection(
            id = "spatial_puzzle",
            dimensionId = "spatial",
            title = "Puzzle Bentuk Singkat",
            summary = "Amati strategi yang kamu gunakan saat menghubungkan bagian dengan keseluruhan.",
            steps = listOf(
                "Gambar persegi lalu bagi menjadi 4–6 bentuk sederhana.",
                "Bayangkan atau susun kembali bagian itu menjadi bentuk lain.",
                "Perhatikan apakah kamu lebih mengandalkan sisi, sudut, atau gambaran keseluruhan."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("shape", "Bentuk yang kamu buat", "Bentuk awal dan bentuk akhirnya apa?"),
                GrowthReflectionPrompt("strategy", "Strategi yang dipakai", "Apa yang paling kamu perhatikan saat menyusun: sisi, sudut, ukuran, atau pola?", 8),
                GrowthReflectionPrompt("insight", "Apa yang kamu sadari?", "Bagian mana yang paling membingungkan dan apa yang akhirnya membantu?", 8)
            )
        ),
        reflection(
            id = "spatial_viewpoint",
            dimensionId = "spatial",
            title = "Ubah Sudut Pandang",
            summary = "Latih membayangkan objek dari posisi pengamat yang berbeda.",
            steps = listOf(
                "Pilih satu objek di sekitar.",
                "Amati tampilannya dari depan.",
                "Tanpa memindahkan objek, bayangkan tampilannya dari samping atau atas."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("object", "Objek", "Objek apa yang kamu amati?"),
                GrowthReflectionPrompt("difference", "Perubahan yang kamu bayangkan", "Bagian apa yang akan terlihat berbeda dari sudut lain?", 8),
                GrowthReflectionPrompt("insight", "Bandingkan dengan kenyataan", "Setelah mengecek posisi sebenarnya, bagian mana yang sesuai atau meleset dari bayanganmu?", 8)
            )
        ),

        challenge(
            id = "social_listen",
            dimensionId = "social",
            title = "Respons Mendengar Aktif",
            summary = "Latih memilih respons yang membantu memahami lawan bicara sebelum memberi solusi.",
            steps = listOf(
                "Baca situasi tanpa menebak motif orang lain.",
                "Utamakan pemahaman sebelum memberi saran.",
                "Cari respons yang memeriksa kembali apa yang kamu dengar."
            ),
            challenge = GrowthChallenge(
                prompt = "Teman berkata, “Aku kewalahan karena tugas menumpuk dan bingung mulai dari mana.” Respons mana yang paling mencerminkan mendengar aktif?",
                choices = listOf(
                    GrowthChoice("a", "“Kamu harusnya dari kemarin bikin jadwal.”"),
                    GrowthChoice("b", "“Sudah, jangan dipikirkan terlalu banyak.”"),
                    GrowthChoice("c", "“Kalau aku tangkap, kamu merasa kewalahan karena semuanya terasa mendesak. Bagian mana yang paling berat sekarang?”"),
                    GrowthChoice("d", "“Aku juga pernah lebih sibuk dari itu.”")
                ),
                correctValue = "c",
                successExplanation = "Respons itu memparafrasekan inti pengalaman lawan bicara dan mengajukan pertanyaan lanjutan, tanpa langsung menghakimi atau mengambil alih masalahnya.",
                retryHint = "Cari respons yang menunjukkan pemahaman dan mengundang klarifikasi, bukan langsung memberi nasihat."
            )
        ),
        reflection(
            id = "social_open_conversation",
            dimensionId = "social",
            title = "Mulai Percakapan Singkat",
            summary = "Amati apa yang membuatmu nyaman atau tidak nyaman ketika memulai interaksi berisiko rendah.",
            steps = listOf(
                "Pilih seseorang yang biasa kamu temui tetapi jarang diajak bicara.",
                "Mulai dengan pertanyaan sederhana yang relevan dengan situasi.",
                "Ajukan satu pertanyaan lanjutan lalu tutup percakapan dengan sopan."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("context", "Situasi", "Dengan siapa dan dalam konteks apa kamu mencoba?"),
                GrowthReflectionPrompt("opening", "Pembuka yang kamu gunakan", "Apa kalimat atau pertanyaan pembukanya?", 5),
                GrowthReflectionPrompt("insight", "Apa yang kamu sadari?", "Apa yang terasa mudah/sulit dan bagaimana respons lawan bicara?", 8)
            )
        ),
        reflection(
            id = "social_perspective",
            dimensionId = "social",
            title = "Catatan Perspektif",
            summary = "Latih mendeskripsikan sudut pandang berbeda tanpa buru-buru menilainya.",
            steps = listOf(
                "Pilih satu perbedaan pendapat yang kamu temui.",
                "Tuliskan posisi orang lain tanpa menyebutnya benar atau salah.",
                "Cari dua alasan masuk akal yang mungkin mendasari posisi tersebut."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("difference", "Perbedaan pendapat", "Apa topik atau situasinya?"),
                GrowthReflectionPrompt("other_view", "Sudut pandang orang lain", "Tuliskan seadil mungkin tanpa menilai.", 8),
                GrowthReflectionPrompt("reasons", "Dua alasan yang mungkin", "Apa dua alasan masuk akal dari sudut pandang mereka?", 8)
            )
        ),

        challenge(
            id = "practical_priority",
            dimensionId = "practical",
            title = "Prioritas Nyata",
            summary = "Latih memilih tindakan berdasarkan dampak dan batas waktu, bukan hanya yang paling mudah.",
            steps = listOf(
                "Perhatikan batas waktu dan dampak setiap tugas.",
                "Pisahkan tugas mendesak dari tugas yang sekadar mudah.",
                "Pilih tindakan pertama yang paling rasional."
            ),
            challenge = GrowthChallenge(
                prompt = "Kamu punya 30 menit. Mana yang paling masuk akal dikerjakan lebih dulu?",
                choices = listOf(
                    GrowthChoice("a", "Merapikan folder laptop yang tidak punya tenggat."),
                    GrowthChoice("b", "Mengirim formulir yang batas waktunya 20 menit lagi dan butuh 10 menit untuk selesai."),
                    GrowthChoice("c", "Menonton video referensi yang mungkin berguna minggu depan."),
                    GrowthChoice("d", "Mengganti wallpaper ponsel.")
                ),
                correctValue = "b",
                successExplanation = "Formulir memiliki batas waktu paling dekat, waktu pengerjaannya masih muat dalam 30 menit, dan konsekuensi menundanya paling jelas.",
                retryHint = "Bandingkan urgensi, waktu yang tersedia, dan konsekuensi jika tugas tidak dikerjakan sekarang."
            )
        ),
        reflection(
            id = "practical_backup",
            dimensionId = "practical",
            title = "Rencana Cadangan",
            summary = "Amati apakah rencana cadanganmu benar-benar merespons risiko yang paling mungkin.",
            steps = listOf(
                "Pilih satu rencana penting untuk 1–3 hari ke depan.",
                "Tulis satu risiko yang paling mungkin mengganggunya.",
                "Siapkan satu tindakan cadangan yang realistis."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("plan", "Rencana utama", "Apa rencana yang ingin kamu jalankan?"),
                GrowthReflectionPrompt("risk", "Risiko paling mungkin", "Apa yang paling realistis bisa mengganggu?", 5),
                GrowthReflectionPrompt("backup", "Tindakan cadangan", "Apa yang akan kamu lakukan jika risiko itu terjadi?", 8)
            )
        ),
        reflection(
            id = "practical_review",
            dimensionId = "practical",
            title = "Review Keputusan",
            summary = "Latih mengevaluasi keputusan dari hasil nyata, bukan hanya perasaan sesudahnya.",
            steps = listOf(
                "Pilih satu keputusan kecil yang sudah kamu ambil.",
                "Bandingkan hasil yang kamu harapkan dengan hasil yang benar-benar terjadi.",
                "Tentukan satu hal yang akan dipertahankan dan satu yang akan diubah."
            ),
            prompts = listOf(
                GrowthReflectionPrompt("decision", "Keputusan yang direview", "Keputusan apa yang kamu ambil?"),
                GrowthReflectionPrompt("expected_actual", "Harapan vs hasil", "Apa yang kamu harapkan dan apa yang sebenarnya terjadi?", 8),
                GrowthReflectionPrompt("insight", "Pelajaran berikutnya", "Apa yang akan kamu pertahankan dan ubah pada situasi serupa?", 8)
            )
        )
    )

    fun forDimension(dimensionId: String): List<GrowthExercise> =
        exercises.filter { it.dimensionId == dimensionId }
}
