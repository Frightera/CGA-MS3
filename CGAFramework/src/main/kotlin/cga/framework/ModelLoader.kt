package cga.framework

import cga.exercise.components.geometry.Material
import cga.exercise.components.geometry.Mesh
import cga.exercise.components.geometry.Renderable
import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.texture.Texture2D
import org.joml.Matrix3f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.*
import org.lwjgl.opengl.GL11
import java.nio.IntBuffer
import java.util.*

object ModelLoader {
    private fun load(objPath: String): RawModel? {
        val rm = RawModel()
        try {
            val aiScene = Assimp.aiImportFile(objPath, Assimp.aiProcess_Triangulate or Assimp.aiProcess_GenNormals)
                    ?: return null
            // read materials
            for (m in 0 until aiScene.mNumMaterials()) {
                val rmat = RawMaterial()
                val tpath = AIString.calloc()
                val sceneMat = aiScene.mMaterials() ?: return null
                val mat = AIMaterial.create(sceneMat[m])
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_DIFFUSE, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                // diffuse texture
                var tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.diffTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.diffTexIndex = rm.textures.size - 1
                }
                // specular texture
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_SPECULAR, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.specTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.specTexIndex = rm.textures.size - 1
                }
                // emissive texture
                Assimp.aiGetMaterialTexture(mat, Assimp.aiTextureType_EMISSIVE, 0, tpath, null as IntBuffer?, null, null, null, null, null)
                tpathj = tpath.dataString()
                if (rm.textures.contains(tpathj)) rmat.emitTexIndex = rm.textures.indexOf(tpathj) else {
                    rm.textures.add(tpathj)
                    rmat.emitTexIndex = rm.textures.size - 1
                }
                // shininess
                val sptr = PointerBuffer.allocateDirect(1)
                Assimp.aiGetMaterialProperty(mat, Assimp.AI_MATKEY_SHININESS, sptr)
                val sprop = AIMaterialProperty.create(sptr[0])
                rmat.shininess = sprop.mData().getFloat(0)
                rm.materials.add(rmat)
            }
            // read meshes
            val meshes = ArrayList<RawMesh>()
            for (m in 0 until aiScene.mNumMeshes()) {
                val sceneMeshes = aiScene.mMeshes() ?: return null
                val aiMesh = AIMesh.create(sceneMeshes[m])
                val mesh = RawMesh()
                // read vertices
                for (v in 0 until aiMesh.mNumVertices()) {
                    val aiVert = aiMesh.mVertices()[v]
                    val sceneNormals = aiMesh.mNormals() ?: return null
                    val aiNormal = sceneNormals[v]
                    val sceneTextureCoords = aiMesh.mTextureCoords(0) ?: return null
                    val aiTexCoord = if (aiMesh.mNumUVComponents(0) > 0) sceneTextureCoords[v] else null
                    val vert = Vertex(
                            Vector3f(aiVert.x(), aiVert.y(), aiVert.z()),
                            if (aiTexCoord != null) Vector2f(aiTexCoord.x(), aiTexCoord.y()) else Vector2f(0.0f, 0.0f),
                            Vector3f(aiNormal.x(), aiNormal.y(), aiNormal.z())
                    )
                    mesh.vertices.add(vert)
                }
                // read indices
                for (f in 0 until aiMesh.mNumFaces()) {
                    val face = aiMesh.mFaces()[f]
                    for (i in 0 until face.mNumIndices()) {
                        mesh.indices.add(face.mIndices()[i])
                    }
                }
                // material index
                mesh.materialIndex = aiMesh.mMaterialIndex()
                meshes.add(mesh)
            }
            // traverse assimp scene graph
            val nodeQueue: Queue<AINode> = LinkedList()
            nodeQueue.offer(aiScene.mRootNode())
            while (!nodeQueue.isEmpty()) {
                val node = nodeQueue.poll()
                for (m in 0 until node.mNumMeshes()) {
                    var sceneMeshes = node.mMeshes() ?: return null
                    rm.meshes.add(meshes[sceneMeshes[m]])
                }
                for (c in 0 until node.mNumChildren()) {
                    val sceneChildren = node.mChildren() ?: return null
                    val cnode = AINode.create(sceneChildren[c])
                    nodeQueue.offer(cnode)
                }
            }
        } catch (ex: Exception) {
            throw Exception("Something went terribly wrong. Thanks java.\n" + ex.message)
        }
        return rm
    }

    private fun flattenVertexData(vertices: List<Vertex>, rot: Matrix3f): FloatArray {
        val data = FloatArray(8 * vertices.size)
        var di = 0
        for ((position, texCoord, normal) in vertices) {
            position.mul(rot)
            normal.mul(Matrix3f(rot).transpose().invert())
            data[di++] = position.x
            data[di++] = position.y
            data[di++] = position.z
            data[di++] = texCoord.x
            data[di++] = texCoord.y
            data[di++] = normal.x
            data[di++] = normal.y
            data[di++] = normal.z
        }
        return data
    }

    private fun flattenIndexData(indices: List<Int>): IntArray {
        val data = IntArray(indices.size)
        var di = 0
        for (i in indices) {
            data[di++] = i
        }
        return data
    }

    fun loadModel(objpath: String, pitch: Float, yaw: Float, roll: Float): Renderable? {
        val model = load(objpath) ?: return null
        val textures = ArrayList<Texture2D>()
        val materials = ArrayList<Material>()
        val meshes = ArrayList<Mesh>()
        val stride = 8 * 4
        val atr1 = VertexAttribute(3, GL11.GL_FLOAT, stride, 0)
        val atr2 = VertexAttribute(2, GL11.GL_FLOAT, stride, 3 * 4)
        val atr3 = VertexAttribute(3, GL11.GL_FLOAT, stride, 5 * 4)
        val vertexAttributes = arrayOf(atr1, atr2, atr3)
        // preprocessing rotation
        val rot = Matrix3f().rotateZ(roll).rotateY(yaw).rotateX(pitch)
        // create textures
//default textures
        val ddata = BufferUtils.createByteBuffer(4)
        ddata.put(0.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte())
        ddata.flip()
        for (i in model.textures.indices) {
            if (model.textures[i].isEmpty()) {
                textures.add(Texture2D(ddata, 1, 1, true))
            } else {
                textures.add(Texture2D(objpath.substring(0, objpath.lastIndexOf('/') + 1) + model.textures[i], true))
            }
        }
        // materials
        for (i in model.materials.indices) {
            materials.add(Material(textures[model.materials[i].diffTexIndex],
                    textures[model.materials[i].emitTexIndex],
                    textures[model.materials[i].specTexIndex],
                    model.materials[i].shininess,
                    Vector2f(1.0f, 1.0f)))
        }
        // meshes
        for (i in model.meshes.indices) {
            meshes.add(Mesh(flattenVertexData(model.meshes[i].vertices, rot),
                    flattenIndexData(model.meshes[i].indices),
                    vertexAttributes,
                    materials[model.meshes[i].materialIndex]))
        }
        // assemble the renderable
        return Renderable(meshes)
    }
}