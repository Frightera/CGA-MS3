package cga.exercise.game

import cga.exercise.components.camera.TronCamera
import cga.exercise.components.geometry.Material
import cga.exercise.components.geometry.Mesh
import cga.exercise.components.geometry.Renderable
import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import org.joml.*
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.INTELBlackholeRender
import java.awt.MenuShortcut
import java.nio.file.Paths


/**
 * Created by Fabian on 16.09.2017.
 */
class Scene(private val window: GameWindow) {
    private val staticShader: ShaderProgram
    private val tronShader: ShaderProgram
    val sphereObj : Mesh
    val groundObj : Mesh
    var ground :Renderable
    var sphere : Renderable
    var camera : TronCamera
    var groundMaterial : Material
    var motorRad : Renderable?


    //scene setup
    init {

        //-------------------------------ObjLoader--------------------------------------------
        motorRad = ModelLoader.loadModel("assets/Light Cycle/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90f),Math.toRadians(90f),0f )
        motorRad?.scaleLocal(Vector3f(0.8f))

        //Sphere

        val res : OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/sphere.obj")
        //Get the first mesh of teh first object
        val objM : OBJLoader.OBJMesh = res.objects[0].meshes[0]

        val stride : Int = 8 * 4
        val attrPos = VertexAttribute(3, GL11.GL_FLOAT,stride,0)   //Position
        val attrTC = VertexAttribute(2, GL11.GL_FLOAT, stride, 3*4)  //Texture Coordinate
        val attrNorm = VertexAttribute(3, GL11.GL_FLOAT,stride,5*4)   //NormalVal

        val objVerAtt = arrayOf<VertexAttribute>(attrPos,attrTC,attrNorm)
        sphereObj = Mesh(objM.vertexData,objM.indexData, objVerAtt)

        //GroundTexture
        val emitTexture : Texture2D = Texture2D("assets/textures/ground_emit.png",true)
        val diffTexture : Texture2D = Texture2D("assets/textures/ground_diff.png",true)
        val specTexture : Texture2D = Texture2D("assets/textures/ground_spec.png",true)
        emitTexture.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR)
        diffTexture.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR)
        specTexture.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR)

        groundMaterial = Material(diffTexture ,emitTexture,specTexture,60f , Vector2f(64.0f, 64.0f))

        //Ground
        val res2 : OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/ground.obj")
        val objG : OBJLoader.OBJMesh = res2.objects[0].meshes[0]

        val strideGround : Int = 8 * 4
        val attrPosGround = VertexAttribute(3, GL11.GL_FLOAT,strideGround,0)
        val attrTCGround = VertexAttribute(2, GL11.GL_FLOAT,strideGround,3*4)
        val attrNormGround = VertexAttribute(3, GL11.GL_FLOAT, strideGround,5*4)

        val objVerAttGround = arrayOf<VertexAttribute>(attrPosGround,attrTCGround,attrNormGround)


        groundObj = Mesh(objG.vertexData,objG.indexData,objVerAttGround,groundMaterial)


        //-----------------------------MS2 Transformation----------------------------

        ground = Renderable(mutableListOf(groundObj))
        //ground.scaleLocal(Vector3f(0.03f))
        //ground.rotateLocal(90f,0f,0f)

        sphere = Renderable(mutableListOf(sphereObj))
        //sphere.scaleLocal(Vector3f(0.5f))

        //------------------------------Camera---------------------------------------------

        camera = TronCamera()
        //camera.rotateLocal(Math.toRadians(-20f),0f,0f)
        camera.rotateLocal(Math.toRadians(-35f),0f,0f)
        camera.translateLocal(Vector3f(0f,0f,4f))
        camera.parent = motorRad

        //------------------------------Texture--------------------------------



        //--------------------------------------------------------------------------------
        tronShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")

        staticShader = ShaderProgram("assets/shaders/simple_vert.glsl", "assets/shaders/simple_frag.glsl")

        //initial opengl state
        //glClearColor(0.6f, 1.0f, 1.0f, 1.0f); GLError.checkThrow() Blau
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); GLError.checkThrow()
        //glDisable(GL_CULL_FACE); GLError.checkThrow()

        GL11.glDisable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()

        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()
    }

    fun render(dt: Float, t: Float) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        tronShader.use()

        camera.bind(tronShader)
        ground.render(tronShader)
        motorRad?.render(tronShader)


    }

    fun update(dt: Float, t: Float) {
        if (window.getKeyState(GLFW_KEY_W)){
            motorRad?.translateLocal((Vector3f(0f,0f,-5f*dt)))
            if (window.getKeyState(GLFW_KEY_A)){
                motorRad?.rotateLocal(0f,1f*dt,0f)
                //motorRad?.translateLocal((Vector3f(0f,0f,-2f*dt)))
            }
            if (window.getKeyState(GLFW_KEY_D)){
                motorRad?.rotateLocal(0f,-1f*dt,0f)
                //motorRad?.translateLocal((Vector3f(0f,0f,-2f*dt)))
            }
        }
        else if (window.getKeyState(GLFW_KEY_S)){
            motorRad?.translateLocal(Vector3f(0f,0f,5f*dt))
            if (window.getKeyState(GLFW_KEY_A)){
                motorRad?.rotateLocal(0f,-1f*dt,0f)
            }
            if (window.getKeyState(GLFW_KEY_D)){
                motorRad?.rotateLocal(0f,1f*dt,0f)
            }
        }
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {}

    fun cleanup() {}
}
