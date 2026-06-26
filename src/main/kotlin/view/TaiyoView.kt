package view

import interfaces.Taiyo

interface TaiyoView {
    fun getModel(): Taiyo
    fun notifyModelChanged()
}