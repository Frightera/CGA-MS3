package cga.exercise.components.texture

import java.nio.ByteBuffer

interface ITexture {
    fun processTexture(imageData: ByteBuffer, width: Int, height: Int, genMipMaps: Boolean)

    fun setTexParams(wrapS: Int, wrapT: Int, minFilter: Int, magFilter: Int)

    fun bind(textureUnit: Int)
    fun unbind()

    fun cleanup()
}