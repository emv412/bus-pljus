package com.buspljus

data class BgVozStation(
    val id: String,
    val naziv: String,
    val redvoznje: String,
    val distance: Double
)

data class SacuvanaStanica(
    val tabela: String,
    val kolone: Array<String>,
    val odabir: String,
    val parametri: Array<String>,
    val redjanjepo: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SacuvanaStanica

        if (tabela != other.tabela) return false
        if (!kolone.contentEquals(other.kolone)) return false
        if (odabir != other.odabir) return false
        if (!parametri.contentEquals(other.parametri)) return false
        if (redjanjepo != other.redjanjepo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tabela.hashCode()
        result = 31 * result + kolone.contentHashCode()
        result = 31 * result + odabir.hashCode()
        result = 31 * result + parametri.contentHashCode()
        result = 31 * result + (redjanjepo?.hashCode() ?: 0)
        return result
    }
}

data class LineInfo(val lineId: String, val destination: String)