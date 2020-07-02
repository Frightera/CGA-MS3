package cga.exercise.components.geometry


import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.opengl.*
import org.lwjgl.opengl.ARBVertexArrayObject.glBindVertexArray
import org.lwjgl.opengl.ARBVertexArrayObject.glGenVertexArrays
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import java.nio.IntBuffer

/**
 * Creates a Mesh object from vertexdata, intexdata and a given set of vertex attributes
 *
 * @param vertexdata plain float array of vertex data
 * @param indexdata  index data
 * @param attributes vertex attributes contained in vertex data
 * @throws Exception If the creation of the required OpenGL objects fails, an exception is thrown
 *
 * Created by Fabian on 16.09.2017.
 */
class Mesh(vertexdata: FloatArray, indexdata: IntArray, attributes: Array<VertexAttribute>, material: Material?=null) {
    //private data
    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var indexcount = 0
    private var mat = material

    private var length = indexdata.size


    init {
        // todo: place your code here

        // todo: generate IDs
        // todo: bind your objects
        // todo: upload your mesh data

        vao = glGenVertexArrays()
        glBindVertexArray(vao)

        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER,vbo)
        glBufferData(GL_ARRAY_BUFFER,vertexdata, GL_STATIC_DRAW)

        for (elements in attributes){
            glEnableVertexAttribArray(indexcount)
            glVertexAttribPointer(indexcount, attributes[indexcount].n, attributes[indexcount].type,
                    false, attributes[indexcount].stride, attributes[indexcount].offset.toLong())
            indexcount++
        }

        ibo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ibo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,indexdata, GL_STATIC_DRAW)



        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER,0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,0)

    }

    /**
     * renders the mesh
     */
    private fun render() {
        // todo: place your code here
        // call the rendering method every frame
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES,length, GL_UNSIGNED_INT,0)
        glBindVertexArray(0)
    }

    fun render(shaderProgram: ShaderProgram){
        mat?.bind(shaderProgram)
        render()
    }

    /**
     * Deletes the previously allocated OpenGL objects for this mesh
     */
    fun cleanup() {
        if (ibo != 0) GL15.glDeleteBuffers(ibo)
        if (vbo != 0) GL15.glDeleteBuffers(vbo)
        if (vao != 0) GL30.glDeleteVertexArrays(vao)
    }
}