package cga.framework

data class RawMesh(
    var vertices: MutableList<Vertex> = mutableListOf(),
    var indices: MutableList<Int> = mutableListOf(),
    var materialIndex: Int = 0)
