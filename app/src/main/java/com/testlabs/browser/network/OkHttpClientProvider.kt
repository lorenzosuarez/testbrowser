/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * OkHttp client with system DNS and DoH fallbacks to avoid -105 resolution errors.
 */
public object OkHttpClientProvider {
    @Volatile private var cached: OkHttpClient? = null

    public fun client(uaCh: UserAgentClientHintsManager): OkHttpClient =
        cached ?: synchronized(this) {
            cached ?: build(uaCh).also { cached = it }
        }

    private fun build(uaCh: UserAgentClientHintsManager): OkHttpClient {
        val base = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .addInterceptor(uaParityInterceptor(uaCh))
            .build()

        val dohCloudflare = DnsOverHttps.Builder()
            .client(base)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1")
            )
            .includeIPv6(true)
            .build()

        val dohGoogle = DnsOverHttps.Builder()
            .client(base)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .includeIPv6(true)
            .build()

        val multiDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                try { return Dns.SYSTEM.lookup(hostname) } catch (_: Exception) {}
                try { return dohCloudflare.lookup(hostname) } catch (_: Exception) {}
                return dohGoogle.lookup(hostname)
            }
        }

        return base.newBuilder()
            .dns(multiDns)
            .build()
    }

    private fun uaParityInterceptor(uaCh: UserAgentClientHintsManager) = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()

        // Remove X-Requested-With
        original.headers.names()
            .filter { it.equals("x-requested-with", ignoreCase = true) }
            .forEach { builder.removeHeader(it) }

        // Remove any existing UA-CH headers
        listOf("sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform").forEach { h ->
            original.headers.names()
                .firstOrNull { it.equals(h, ignoreCase = true) }
                ?.let { builder.removeHeader(it) }
        }

        // Inject canonical UA-CH
        uaCh.asMap(isMobile = true).forEach { (k, v) -> builder.addHeader(k, v) }

        chain.proceed(builder.build())
    }
}