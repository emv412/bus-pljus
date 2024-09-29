package com.buspljus
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build

fun ProveraDostupnostiInterneta(context: Context): Boolean {
    val menadzer = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = menadzer.activeNetwork ?: return false
        val mrezneMogucnosti = menadzer.getNetworkCapabilities(network) ?: return false
        mrezneMogucnosti.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                mrezneMogucnosti.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    } else {
        val activeNetwork: NetworkInfo? = menadzer.activeNetworkInfo
        activeNetwork?.isConnectedOrConnecting == true
    }
}
