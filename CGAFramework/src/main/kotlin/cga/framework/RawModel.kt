package cga.framework

data class RawModel(
        var meshes: MutableList<RawMesh> = mutableListOf(),
        var materials: MutableList<RawMaterial> = mutableListOf(),
        var textures: MutableList<String> = mutableListOf()
)