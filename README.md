# Payment Gateway Service

Proyek ini adalah implementasi dari sebuah layanan Payment Gateway sebagai bagian dari *Coding Test - Java Backend Developer*.

## Deskripsi
Layanan ini dibangun menggunakan **Spring Boot** dan dirancang untuk berfungsi sebagai perantara yang mengorkestrasi alur transaksi pembayaran. Ia dapat menerima permintaan dari berbagai channel (Mobile Banking, ATM, dll.), melakukan proses debit ke sistem perbankan inti, dan meneruskan permintaan pembayaran ke Biller Aggregator.

Sistem ini juga dilengkapi dengan fitur-fitur modern yang tangguh, termasuk:
- **Keamanan**: Otentikasi berbasis **JWT** untuk semua endpoint bisnis.
- **Resilience**: Pola **Circuit Breaker** (menggunakan Resilience4j) untuk menangani kegagalan pada layanan eksternal.
- **Arsitektur Asinkron**: Publikasi *event* ke **Apache Kafka** setelah transaksi berhasil untuk diproses oleh layanan lain (*downstream services*).
- **Containerization**: Siap dijalankan di mana saja menggunakan **Docker** dan **Docker Compose**.

## Arsitektur & Alur Kerja
Berikut adalah diagram sekuens yang menggambarkan alur transaksi pembayaran utama:

![Alur Transaksi Payment Gateway](sequence-diagram.png)


## Teknologi yang Digunakan
- **Framework**: Spring Boot 3.5.6
- **Bahasa**: Java 21
- **Keamanan**: Spring Security 6 (Otentikasi JWT)
- **Database**: PostgreSQL dengan Spring Data JPA (Hibernate) & Flyway (untuk migrasi)
- **Integrasi**:
  - Spring Cloud OpenFeign (REST Client)
  - Resilience4j (Circuit Breaker)
  - Apache Kafka (Message Broker)
- **Dokumentasi**: Swagger/OpenAPI v3 (springdoc-openapi)
- **Pengujian**: JUnit 5, Mockito, H2 Database, JaCoCo
- **Container**: Docker & Docker Compose
- **Build Tool**: Maven

## Prasyarat
- Java 21
- Docker & Docker Compose
- Maven

## Cara Menjalankan Aplikasi
Cara termudah dan direkomendasikan untuk menjalankan keseluruhan aplikasi beserta semua layanannya (PostgreSQL & Kafka) adalah dengan menggunakan Docker Compose.

1.  **Clone Repositori Ini**
    ```bash
    git clone https://github.com/frddyy/payment-gateway-cip.git
    cd payment-gateway-cip
    ```

2.  **Pastikan Docker Berjalan**
    Buka aplikasi Docker Desktop di mesin Anda.

3.  **Jalankan Docker Compose**
    Buka terminal di direktori *root* proyek dan jalankan perintah berikut. Perintah ini akan membangun *image* untuk aplikasi dan menjalankan semua *container* yang dibutuhkan.
    ```bash
    docker compose up --build
    ```
    Tunggu beberapa saat hingga semua layanan berjalan. Aplikasi akan tersedia di `http://localhost:8080`.

## Menjalankan Tes & Melihat Laporan Cakupan Kode (Code Coverage)
Proyek ini dikonfigurasi dengan JaCoCo untuk mengukur cakupan kode dari unit dan integration test.

1.  **Jalankan Tes dan Buat Laporan**
    Dari direktori root proyek, jalankan perintah Maven berikut:
    ```bash
    mvn clean test
    ```
    Perintah ini akan menjalankan semua tes dan menghasilkan laporan JaCoCo.

2.  **Lihat Laporan**
    Setelah perintah di atas selesai, laporan akan tersedia dalam format HTML. Buka file berikut di browser Anda:
    `payment-gateway-cip/target/site/jacoco/index.html`

## Dokumentasi & Pengujian API
Dokumentasi API yang interaktif dan lengkap tersedia melalui Swagger UI.

- **URL Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Cara Menguji Endpoint yang Diamankan
1.  **Dapatkan Token Tes**: Akses `GET http://localhost:8080/api/test/token/{username}` (ganti `{username}` dengan nama Anda) via browser atau Swagger. Copy string token yang dihasilkan.

2.  **Atur Otorisasi**: Di halaman Swagger UI, klik tombol hijau **"Authorize"**. Di dialog yang muncul, masukkan `Bearer <token_anda>`. Klik "Authorize".

3.  **Jalankan Skenario**: Sekarang Anda bisa menjalankan skenario-skenario tes berikut di endpoint `POST /api/payments`.

#### Skenario Sukses
Gunakan body ini untuk alur yang berhasil sepenuhnya.
```json
{
  "orderId": "INV-SUCCESS-001",
  "channel": "MOBILE_BANKING",
  "amount": 150000,
  "currency": "IDR",
  "paymentMethod": "VIRTUAL_ACCOUNT",
  "account": "1234567890"
}
```

#### Skenario Gagal Debit
Gunakan `account` yang mengandung kata `fail` untuk mensimulasikan kegagalan dari Core Banking.
```json
{
  "orderId": "INV-DEBIT-FAIL-002",
  "channel": "ATM",
  "amount": 500000,
  "currency": "IDR",
  "paymentMethod": "DEBIT_CARD",
  "account": "123-FAIL-ACCOUNT"
}
```

#### Skenario Gagal Biller
Gunakan `orderId` yang mengandung kata `fail` untuk mensimulasikan kegagalan dari Biller Aggregator setelah proses debit berhasil.
```json
{
  "orderId": "INV-BILLER-FAIL-003",
  "channel": "INTERNET_BANKING",
  "amount": 75000,
  "currency": "IDR",
  "paymentMethod": "CREDIT_CARD",
  "account": "9876543210"
}
```

## Konfigurasi
Konfigurasi utama aplikasi terdapat di `src/main/resources/application.properties`. Untuk menjalankan dengan Docker, beberapa properti akan di-override oleh *environment variables* yang didefinisikan di `docker-compose.yml`. File `application.properties.example` disediakan sebagai template konfigurasi.