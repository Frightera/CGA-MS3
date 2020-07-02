package cga.framework

import org.joml.Math
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created by Fabian on 16.09.2017.
 */
object OBJLoader {

    private const val OBJECT_SHELL_SIZE = 8
    private const val OBJREF_SIZE = 4
    private const val LONG_FIELD_SIZE = 8
    private const val INT_FIELD_SIZE = 4
    private const val SHORT_FIELD_SIZE = 2
    private const val CHAR_FIELD_SIZE = 2
    private const val BYTE_FIELD_SIZE = 1
    private const val BOOLEAN_FIELD_SIZE = 1
    private const val DOUBLE_FIELD_SIZE = 8
    private const val FLOAT_FIELD_SIZE = 4

    fun loadOBJ(objpath: String, recenterObjects: Boolean = false, normalizeObjects: Boolean = false): OBJResult {
        val result = OBJResult()
        return try {
            val objFile = File(objpath)
            val stream = BufferedInputStream(FileInputStream(objFile))
            val scanner = Scanner(stream)
            scanner.useLocale(Locale.US)
            val cache = DataCache()
            while (scanner.hasNext()) {
                if (scanner.hasNext("o") || scanner.hasNext("v") || scanner.hasNext("vt") || scanner.hasNext("vn") || scanner.hasNext("g") || scanner.hasNext("f")) {
                    result.objects.add(parseObject(cache, scanner))
                } else {
                    scanner.nextLine()
                }
            }
            result.name = objFile.name
            stream.close()
            if (recenterObjects) {
                for (obj in result.objects) {
                    obj.recenter()
                }
            }
            if (normalizeObjects) {
                for (obj in result.objects) {
                    obj.normalize()
                }
            }
            result
        } catch (ex: Exception) {
            throw OBJException("Error reading OBJ file:\n" + ex.message)
        }
    }

    fun recalculateNormals(mesh: OBJMesh) {
        try {
            for (i in mesh.vertices.indices)  //initialize all Vertex normals with nullvectors
            {
                mesh.vertices[i].normal = Vector3f(0.0f, 0.0f, 0.0f)
            }
            run {
                var i = 0
                while (i < mesh.indices.size) {
                    val v1 = Vector3f(mesh.vertices[mesh.indices[i]].position)
                    val v2 = Vector3f(mesh.vertices[mesh.indices[i + 1]].position)
                    val v3 = Vector3f(mesh.vertices[mesh.indices[i + 2]].position)
                    //counter clockwise winding
                    val edge1 = Vector3f()
                    v2.sub(v1, edge1)
                    val edge2 = Vector3f()
                    v3.sub(v1, edge2)
                    val normal = Vector3f()
                    edge1.cross(edge2, normal)
                    //for each Vertex all corresponding normals are added. The result is a non unit length vector which is the average direction of all assigned normals.
                    mesh.vertices[mesh.indices[i]].normal.add(normal)
                    mesh.vertices[mesh.indices[i + 1]].normal.add(normal)
                    mesh.vertices[mesh.indices[i + 2]].normal.add(normal)
                    i += 3
                }
            }
            for (i in mesh.vertices.indices)  //normalize all normals calculated in the previous step
            {
                mesh.vertices[i].normal.normalize()
            }
            mesh.hasNormals = true
        } catch (ex: Exception) {
            throw OBJException("Normal calculation failed:\n" + ex.message)
        }
    }

    fun reverseWinding(mesh: OBJMesh) {
        var i = 0
        while (i < mesh.indices.size) {
            val tmp = mesh.indices[i + 1]
            mesh.indices[i + 1] = mesh.indices[i + 2]
            mesh.indices[i + 2] = tmp
            i += 3
        }
    }

    private fun parseObject(cache: DataCache, scanner: Scanner): OBJObject {
        return try {
            val obj = OBJObject()
            val command: String
            if (!scanner.hasNext()) throw OBJException("Error parsing Object.")
            if (scanner.hasNext("o")) {
                command = scanner.next()
                if (scanner.hasNextLine()) obj.name = scanner.nextLine().trim { it <= ' ' } else throw OBJException("Error parsing object name.")
            } else {
                obj.name = "UNNAMED"
            }

            while (scanner.hasNext()) {
                //Fill cache
                if (scanner.hasNext("v")) //position
                {
                    cache.positions.add(parsePosition(scanner))
                } else if (scanner.hasNext("vt")) //uv
                {
                    cache.uvs.add(parseUV(scanner))
                } else if (scanner.hasNext("vn")) //normal
                {
                    cache.normals.add(parseNormal(scanner))
                } else if (scanner.hasNext("g") || scanner.hasNext("f")) //grouped or ungrouped mesh
                {
                    obj.meshes.add(parseMesh(cache, scanner))
                } else if (scanner.hasNext("o")) //next object found
                {
                    return obj
                } else {
                    scanner.nextLine()
                }
            } //eof reached. model should be complete
            obj
        } catch (ex: Exception) {
            throw OBJException("Error parsing object:\n" + ex.message)
        }
    }

    private fun parseVector3(scanner: Scanner): Vector3f {
        val x: Double = scanner.nextDouble()
        val y: Double = scanner.nextDouble()
        val z: Double = scanner.nextDouble()
        return Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }

    private fun parsePosition(scanner: Scanner): Vector3f {
        return try {
            if (!scanner.hasNext("v")) throw OBJException("Error parsing v command.")
            scanner.next()
            parseVector3(scanner)
        } catch (ex: Exception) {
            throw OBJException("Error parsing v command:\n" + ex.message)
        }
    }

    private fun parseNormal(scanner: Scanner): Vector3f {
        return try {
            if (!scanner.hasNext("vn")) throw OBJException("Error parsing vn command.")
            scanner.next()
            parseVector3(scanner)
        } catch (ex: Exception) {
            throw OBJException("Error parsing vn command:\n" + ex.message)
        }
    }

    private fun parseUV(scanner: Scanner): Vector2f {
        return try {
            if (!scanner.hasNext("vt")) throw OBJException("Error parsing vt command.")
            scanner.next()
            val u: Double = scanner.nextDouble()
            val v: Double = scanner.nextDouble()
            Vector2f(u.toFloat(), v.toFloat())
        } catch (ex: Exception) {
            throw OBJException("Error parsing vt command:\n" + ex.message)
        }
    }

    private fun parseMesh(cache: DataCache, scanner: Scanner): OBJMesh {
        return try {
            val mesh = OBJMesh()
            val meshvertset = HashMap<VertexDef, Int>()
            //later create actual vertices out of these and put them into the mesh
            val meshverts = ArrayList<VertexDef>() //for tracking order of insertion
            val meshindices = ArrayList<Int>() //Vertex index of one of the Vertex defs above
            val command: String
            if (scanner.hasNext("g")) //if we have a grouped mesh extract its name first
            {
                command = scanner.next()
                if (scanner.hasNextLine()) {
                    mesh.name = scanner.nextLine().trim { it <= ' ' }
                } else throw OBJException("Error parsing mesh name.")
            } else {
                mesh.name = "UNGROUPED"
            }
            //now process faces
            while (scanner.hasNext()) { //command = scanner.next();
                if (scanner.hasNext("f")) //yay we found a face
                {
                    val face = parseFace(scanner)
                    //process face data and build mesh
                    for (i in 0..2) { //add Vertexdefs and indices
                        if (meshvertset.containsKey(face.verts[i])) //if Vertex def exists already, just get the index and push it onto index array
                        {
                            meshindices.add(meshvertset[face.verts[i]]!!)
                        } else  //if not, push a index pointing to the last pushed Vertex def, push Vertex def and insert it into the set
                        {
                            meshindices.add(meshvertset.size)
                            meshverts.add(face.verts[i])
                            meshvertset[face.verts[i]] = meshverts.size - 1
                        }
                    }
                } else if (scanner.hasNext("g") || scanner.hasNext("o")) //found next mesh group
                {
                    fillMesh(mesh, cache, meshverts, meshindices)
                    return mesh
                } else if (scanner.hasNext("v")) //position
                {
                    cache.positions.add(parsePosition(scanner))
                } else if (scanner.hasNext("vt")) //uv
                {
                    cache.uvs.add(parseUV(scanner))
                } else if (scanner.hasNext("vn")) //normal
                {
                    cache.normals.add(parseNormal(scanner))
                } else {
                    scanner.nextLine()
                }
            }
            fillMesh(mesh, cache, meshverts, meshindices)
            mesh
        } catch (ex: Exception) {
            throw OBJException("Error parsing mesh:\n" + ex.message)
        }
    }

    private fun parseFace(scanner: Scanner): Face {
        return try {
            val command: String
            val face = Face()
            if (scanner.hasNext("f")) {
                command = scanner.next()
                face.verts.add(parseVertex(scanner.next()))
                face.verts.add(parseVertex(scanner.next()))
                face.verts.add(parseVertex(scanner.next()))
                face
            } else {
                throw OBJException("Error parsing face")
            }
        } catch (ex: Exception) {
            throw OBJException("Error parsing Face: " + ex.message)
        }
    }

    private fun parseVertex(vstring: String): VertexDef {
        return try { //buffer for v, vt, vn index
            val att = vstring.split("/".toRegex()).toTypedArray()
            if (att.size != 3) throw OBJException("Error parsing vertex.")
            val vert = VertexDef()
            vert.p_idx = if (att[0].isNotEmpty()) att[0].toInt() - 1 else 0
            vert.p_defined = att[0].isNotEmpty()
            vert.uv_idx = if (att[1].isNotEmpty()) att[1].toInt() - 1 else 0
            vert.uv_defined = att[1].isNotEmpty()
            vert.n_idx = if (att[2].isNotEmpty()) att[2].toInt() - 1 else 0
            vert.n_defined = att[2].isNotEmpty()
            vert
        } catch (ex: Exception) {
            throw OBJException(ex.message)
        }
    }

    private fun fillMesh(mesh: OBJMesh, cache: DataCache, vdefs: ArrayList<VertexDef>, indices: ArrayList<Int>) {
        try { //assemble the mesh from the collected indices
            //create Vertex from cache data
            var hasverts = true
            var hasuvs = true
            var hasnormals = true
            for (i in vdefs.indices) {
                val vert = Vertex()
                if (vdefs[i].p_defined) {
                    if (vdefs[i].p_idx < cache.positions.size) vert.position = Vector3f(cache.positions[vdefs[i].p_idx]) else throw OBJException("Missing position in object definition")
                } else {
                    hasverts = false
                }
                if (vdefs[i].uv_defined) {
                    if (vdefs[i].uv_idx < cache.uvs.size) vert.uv = Vector2f(cache.uvs[vdefs[i].uv_idx]) else throw OBJException("Missing texture coordinate in object definition")
                } else {
                    hasuvs = false
                }
                if (vdefs[i].n_defined) {
                    if (vdefs[i].n_idx < cache.normals.size) vert.normal = Vector3f(cache.normals[vdefs[i].n_idx]) else throw OBJException("Missing normal in object definition")
                } else {
                    hasnormals = false
                }
                mesh.vertices.add(vert)
            }
            mesh.indices = indices
            mesh.hasPositions = hasverts
            mesh.hasUVs = hasuvs
            mesh.hasNormals = hasnormals
        } catch (ex: Exception) {
            throw OBJException("Error filling mesh:\n" + ex.message)
        }
    }

    class OBJException(message: String?) : Exception(message)

    class Vertex {
        var position: Vector3f
        var uv: Vector2f
        var normal: Vector3f

        constructor() {
            position = Vector3f(0.0f, 0.0f, 0.0f)
            uv = Vector2f(0.0f, 0.0f)
            normal = Vector3f(0.0f, 0.0f, 0.0f)
        }

        constructor(position: Vector3f,
                    uv: Vector2f,
                    normal: Vector3f) {
            this.position = position
            this.uv = uv
            this.normal = normal
        }
    }

    class OBJMesh {
        var hasPositions = false
        var hasUVs = false
        var hasNormals = false
        var name: String = ""
        var vertices: MutableList<Vertex> = mutableListOf()
        var indices: MutableList<Int> = mutableListOf()
        val vertexData: FloatArray
            get() {
                val data = FloatArray(8 * vertices.size)
                var di = 0
                for (v in vertices) {
                    data[di++] = v.position.x
                    data[di++] = v.position.y
                    data[di++] = v.position.z
                    data[di++] = v.uv.x
                    data[di++] = v.uv.y
                    data[di++] = v.normal.x
                    data[di++] = v.normal.y
                    data[di++] = v.normal.z
                }
                return data
            }

        val indexData: IntArray
            get() {
                val data = IntArray(indices.size)
                var di = 0
                for (i in indices) {
                    data[di++] = i
                }
                return data
            }

        fun indexCount(): Int {
            return indices.size
        }

    }

    class OBJObject {
        var name: String = ""
        var meshes: MutableList<OBJMesh> = mutableListOf()

        fun recenter() {
            val max = Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE).mul(-1.0f)
            val min = Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
            for (mesh in meshes) {
                for (vert in mesh.vertices) {
                    val p = vert.position
                    max.x = Math.max(max.x, p.x)
                    max.y = Math.max(max.y, p.y)
                    max.z = Math.max(max.z, p.z)
                    min.x = Math.min(min.x, p.x)
                    min.y = Math.min(min.y, p.y)
                    min.z = Math.min(min.z, p.z)
                }
            }
            val midpoint = Vector3f(min).add(Vector3f(max).sub(min).mul(0.5f))
            for (mesh in meshes) {
                for (vert in mesh.vertices) {
                    vert.position.sub(midpoint)
                }
            }
        }

        fun normalize() {
            val max = Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE).mul(-1.0f)
            val min = Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
            for (mesh in meshes) {
                for (vert in mesh.vertices) {
                    val p = vert.position
                    max.x = Math.max(max.x, p.x)
                    max.y = Math.max(max.y, p.y)
                    max.z = Math.max(max.z, p.z)
                    min.x = Math.min(min.x, p.x)
                    min.y = Math.min(min.y, p.y)
                    min.z = Math.min(min.z, p.z)
                }
            }
            val midpoint = Vector3f(min).add(Vector3f(max).sub(min).mul(0.5f))
            val diff = Math.max(max.x - min.x, Math.max(max.y - min.y, max.z - min.z))
            if (diff > 2.0f) {
                val scale = 2.0f / diff
                for (mesh in meshes) {
                    for (vert in mesh.vertices) {
                        vert.position.sub(midpoint)
                        vert.position.mul(scale)
                        vert.position.add(midpoint)
                    }
                }
            }
        }

    }

    class OBJResult {
        var objects: MutableList<OBJObject> = mutableListOf()
        var name: String = ""

    }

    private class DataCache {
        var positions: MutableList<Vector3f> = mutableListOf()
        var uvs: MutableList<Vector2f> = mutableListOf()
        var normals: MutableList<Vector3f> = mutableListOf()
    }

    private class VertexDef {
        var p_idx = 0
        var uv_idx = 0
        var n_idx = 0
        var p_defined = false
        var uv_defined = false
        var n_defined = false
        override fun hashCode(): Int {
            return Objects.hash(p_idx, uv_idx, n_idx)
        }

        override fun equals(obj: Any?): Boolean {
            return obj is VertexDef && p_idx == obj.p_idx && uv_idx == obj.uv_idx && n_idx == obj.n_idx
        }
    }

    private class Face {
        var verts: MutableList<VertexDef> = mutableListOf()
    }
}