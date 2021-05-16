package com.binishmatheww.scanner.listeners

interface PageClickListener {
    fun pageClicked(position: Int)
    fun deletePage(position: Int)
    fun rotate(position: Int, rotation: Int)
    fun crop(position: Int)
    fun ocr(position: Int)
    fun filter(position: Int, key: Float)
    fun up(position: Int)
    fun down(position: Int)
}