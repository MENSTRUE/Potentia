package com.potentia.growth

data class GrowthExercise(
    val id: String,
    val dimensionId: String,
    val title: String,
    val summary: String,
    val estimatedMinutes: Int = 10,
    val steps: List<String>
)

data class GrowthProgress(
    val weekKey: String,
    val dimensionId: String,
    val selectedExerciseId: String? = null,
    val activeExerciseId: String? = null,
    val completedExerciseIds: Set<String> = emptySet(),
    val updatedAt: Long = System.currentTimeMillis()
)

object GrowthCatalog {
    private val exercises = listOf(
        GrowthExercise(
            id = "logical_deduction",
            dimensionId = "logical",
            title = "Deduksi 3 Petunjuk",
            summary = "Latih menarik kesimpulan dari informasi terbatas tanpa menebak.",
            steps = listOf(
                "Pilih satu keputusan sederhana yang sedang kamu pikirkan hari ini.",
                "Tuliskan tiga fakta yang benar-benar kamu ketahui, lalu pisahkan dari asumsi.",
                "Buat satu kesimpulan yang hanya memakai tiga fakta tadi dan cek apakah ada lompatan logika."
            )
        ),
        GrowthExercise(
            id = "logical_argument",
            dimensionId = "logical",
            title = "Susun Alur Argumen",
            summary = "Biasakan membuat urutan alasan sebelum mengambil kesimpulan.",
            steps = listOf(
                "Tulis satu pendapat atau keputusan dalam satu kalimat.",
                "Di bawahnya tulis: alasan utama → bukti/contoh → kemungkinan bantahan.",
                "Perbaiki kesimpulan jika alasan atau buktinya belum cukup kuat."
            )
        ),
        GrowthExercise(
            id = "logical_strategy",
            dimensionId = "logical",
            title = "Rencana Tiga Langkah",
            summary = "Melatih konsekuensi berurutan dan pilihan alternatif.",
            steps = listOf(
                "Pilih satu target kecil yang bisa dilakukan hari ini.",
                "Tuliskan tiga langkah ke depan dan apa yang mungkin menghambat setiap langkah.",
                "Siapkan satu langkah cadangan untuk hambatan yang paling mungkin terjadi."
            )
        ),

        GrowthExercise(
            id = "creative_alternative_uses",
            dimensionId = "creative",
            title = "5 Penggunaan Alternatif",
            summary = "Melatih kelancaran menghasilkan ide tanpa langsung menilai ide pertama.",
            steps = listOf(
                "Ambil satu benda sederhana di dekatmu, misalnya botol, kardus, atau sendok.",
                "Dalam 3 menit, tulis minimal lima penggunaan lain selain fungsi utamanya.",
                "Pilih satu ide paling tidak biasa tetapi masih masuk akal dan jelaskan cara kerjanya."
            )
        ),
        GrowthExercise(
            id = "creative_three_solutions",
            dimensionId = "creative",
            title = "Tiga Solusi Berbeda",
            summary = "Memaksa diri keluar dari solusi pertama yang terasa paling mudah.",
            steps = listOf(
                "Pilih satu masalah kecil yang nyata dalam kegiatanmu hari ini.",
                "Buat tiga solusi yang benar-benar berbeda pendekatannya.",
                "Bandingkan ketiganya dari sisi kemudahan, biaya/waktu, dan kebaruan."
            )
        ),
        GrowthExercise(
            id = "creative_combine",
            dimensionId = "creative",
            title = "Gabungkan Dua Ide",
            summary = "Melatih kemampuan menghubungkan konsep yang awalnya tidak berkaitan.",
            steps = listOf(
                "Pilih dua benda, aplikasi, atau aktivitas yang berbeda.",
                "Bayangkan satu produk atau kebiasaan baru yang menggabungkan keduanya.",
                "Tulis satu manfaat utama dan satu kelemahan dari kombinasi tersebut."
            )
        ),

        GrowthExercise(
            id = "verbal_summary",
            dimensionId = "verbal",
            title = "Ringkas Jadi 3 Kalimat",
            summary = "Melatih menangkap ide inti dan membuang informasi yang tidak penting.",
            steps = listOf(
                "Ambil satu artikel atau bacaan pendek yang sudah kamu miliki.",
                "Tulis satu kalimat untuk ide utama dan dua kalimat untuk alasan/detail terpenting.",
                "Baca ulang dan hapus kata yang tidak menambah makna."
            )
        ),
        GrowthExercise(
            id = "verbal_word_relation",
            dimensionId = "verbal",
            title = "Jaringan Makna Kata",
            summary = "Melatih hubungan makna, sinonim, antonim, dan analogi.",
            steps = listOf(
                "Pilih satu kata yang sering kamu gunakan.",
                "Tulis dua sinonim, dua antonim, dan satu analogi sederhana untuk kata tersebut.",
                "Gunakan salah satu sinonimnya dalam kalimat baru tanpa mengubah makna utama."
            )
        ),
        GrowthExercise(
            id = "verbal_explain_simple",
            dimensionId = "verbal",
            title = "Jelaskan dengan Bahasa Sederhana",
            summary = "Melatih menyusun penjelasan yang runtut dan mudah dipahami.",
            steps = listOf(
                "Pilih satu konsep yang kamu pelajari minggu ini.",
                "Jelaskan konsep itu seolah-olah kepada orang yang belum pernah mempelajarinya.",
                "Batasi penjelasan menjadi maksimal lima kalimat dan cek apakah istilah teknis sudah dijelaskan."
            )
        ),

        GrowthExercise(
            id = "spatial_rotation",
            dimensionId = "spatial",
            title = "Rotasi Mental",
            summary = "Melatih membayangkan orientasi benda setelah diputar.",
            steps = listOf(
                "Pilih benda kecil yang bentuknya tidak simetris, misalnya kunci atau gunting.",
                "Amati 10 detik, lalu tutup mata dan bayangkan benda diputar 90° ke kanan.",
                "Buka mata, putar bendanya sungguhan, lalu bandingkan dengan bayanganmu. Ulangi dua kali."
            )
        ),
        GrowthExercise(
            id = "spatial_puzzle",
            dimensionId = "spatial",
            title = "Puzzle Bentuk Singkat",
            summary = "Melatih melihat hubungan bagian dan keseluruhan.",
            steps = listOf(
                "Ambil kertas dan gambar persegi, lalu bagi menjadi 4–6 bentuk sederhana.",
                "Potong atau bayangkan potongannya, lalu susun kembali menjadi bentuk berbeda.",
                "Catat strategi yang paling cepat membantumu mengenali kecocokan sisi dan sudut."
            )
        ),
        GrowthExercise(
            id = "spatial_viewpoint",
            dimensionId = "spatial",
            title = "Ubah Sudut Pandang",
            summary = "Melatih membayangkan objek dari posisi pengamat yang berbeda.",
            steps = listOf(
                "Pilih satu objek di meja atau ruanganmu.",
                "Gambar/sketsa cepat tampilannya dari depan.",
                "Tanpa memindahkan objek, bayangkan dan sketsa bagaimana bentuknya jika dilihat dari samping atau atas."
            )
        ),

        GrowthExercise(
            id = "social_listen",
            dimensionId = "social",
            title = "Mendengar Aktif 5 Menit",
            summary = "Melatih memahami lawan bicara sebelum buru-buru merespons.",
            steps = listOf(
                "Saat berbicara dengan seseorang, tahan diri untuk tidak memotong selama beberapa menit.",
                "Ulangi inti perkataannya dengan kalimatmu sendiri: ‘Kalau aku tangkap, maksudmu…’.",
                "Ajukan satu pertanyaan lanjutan sebelum memberikan pendapatmu."
            )
        ),
        GrowthExercise(
            id = "social_open_conversation",
            dimensionId = "social",
            title = "Mulai Percakapan Singkat",
            summary = "Melatih inisiatif sosial dalam situasi berisiko rendah.",
            steps = listOf(
                "Pilih seseorang yang biasa kamu temui tetapi jarang kamu ajak bicara.",
                "Mulai dengan pertanyaan sederhana yang relevan dengan situasi saat itu.",
                "Pertahankan percakapan dengan satu pertanyaan lanjutan dan tutup dengan sopan."
            )
        ),
        GrowthExercise(
            id = "social_perspective",
            dimensionId = "social",
            title = "Catatan Perspektif",
            summary = "Melatih melihat alasan di balik sudut pandang yang berbeda.",
            steps = listOf(
                "Pilih satu perbedaan pendapat yang kamu temui hari ini.",
                "Tuliskan posisi orang lain tanpa menyebutnya salah atau benar.",
                "Tuliskan dua alasan masuk akal yang mungkin membuat orang tersebut berpikir demikian."
            )
        ),

        GrowthExercise(
            id = "practical_priority",
            dimensionId = "practical",
            title = "Prioritas 3 Tugas",
            summary = "Melatih memilih tindakan berdasarkan dampak dan batas waktu.",
            steps = listOf(
                "Tulis tiga tugas nyata yang perlu kamu selesaikan.",
                "Beri nilai 1–3 untuk dampak dan 1–3 untuk urgensi setiap tugas.",
                "Kerjakan terlebih dahulu tugas dengan kombinasi dampak + urgensi terbesar."
            )
        ),
        GrowthExercise(
            id = "practical_backup",
            dimensionId = "practical",
            title = "Rencana Cadangan",
            summary = "Melatih kesiapan menghadapi risiko tanpa membuat rencana berlebihan.",
            steps = listOf(
                "Pilih satu rencana penting untuk 1–3 hari ke depan.",
                "Tulis satu risiko yang paling mungkin mengganggu rencana tersebut.",
                "Siapkan satu tindakan cadangan yang realistis jika risiko itu benar-benar terjadi."
            )
        ),
        GrowthExercise(
            id = "practical_review",
            dimensionId = "practical",
            title = "Review Keputusan",
            summary = "Melatih belajar dari hasil nyata, bukan hanya dari perasaan sesudah mengambil keputusan.",
            steps = listOf(
                "Pilih satu keputusan kecil yang sudah kamu ambil minggu ini.",
                "Tulis apa yang kamu harapkan terjadi dan apa yang benar-benar terjadi.",
                "Catat satu hal yang akan kamu pertahankan dan satu hal yang akan kamu ubah pada keputusan serupa berikutnya."
            )
        )
    )

    fun forDimension(dimensionId: String): List<GrowthExercise> =
        exercises.filter { it.dimensionId == dimensionId }
}
