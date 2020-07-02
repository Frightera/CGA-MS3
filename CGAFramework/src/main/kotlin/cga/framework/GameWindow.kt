package cga.framework

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryUtil

/**
 * Created by Fabian on 16.09.2017.
 */
/**
 * Base class for GameWindows using OpenGL for rendering
 */
abstract class GameWindow(
        /**
         * Returns the current width of the window
         * @return width of the window
         */
        var windowWidth: Int,
        /**
         * Returns the current height of the window
         * @return height of the window
         */
        var windowHeight: Int,
        fullscreen: Boolean,
        vsync: Boolean,
        cvmaj: Int,
        cvmin: Int,
        title: String,
        msaasamples: Int,
        updatefrequency: Float
) {
    //inner types
    /**
     * Simple class that holds screen space x and y coordinates of the current mouse position
     */
    class MousePosition(var xpos: Double, var ypos: Double)

    //private data
    private var m_window: Long = 0
    /**
     * Returns the current width of the default frame buffer
     * @return width of the default frame buffer
     */
    var framebufferWidth: Int
        private set
    /**
     * Returns the current height of the default frame buffer
     * @return height of the default frame buffer
     */
    var framebufferHeight: Int
        private set
    private val m_fullscreen: Boolean
    private val m_vsync: Boolean
    private val m_title: String
    private val m_msaasamples: Int
    private val m_updatefrequency: Float
    private val m_cvmaj: Int
    private val m_cvmin: Int
    //GLFW callbacks
    private var m_caps: GLCapabilities? = null
    private val m_keyCallback: GLFWKeyCallback? = null
    private val m_cpCallback: GLFWCursorPosCallback? = null
    private val m_mbCallback: GLFWMouseButtonCallback? = null
    private val m_fbCallback: GLFWFramebufferSizeCallback? = null
    private val m_wsCallback: GLFWWindowSizeCallback? = null
    private val m_debugProc: Callback? = null
    private var m_currentTime: Long = 0

    //Constructors
    /**
     * Initializes a game window object
     * @param width         Desired window width
     * @param height        Desired window height
     * @param fullscreen    Fullscreen mode
     * @param vsync         Use vsync
     * @param cvmaj         Desired major OpenGL version
     * @param cvmin         Desired minor OpenGL version
     * @param title         Window title
     * @param msaasamples       Desired of multisampling samples to use when displaying the default frame buffer
     * @param updatefrequency   Frequency the update method should be called with. 2x the expected frame rate is a good rule of thumb
     */
    init {
        framebufferWidth = windowWidth
        framebufferHeight = windowHeight
        m_fullscreen = fullscreen
        m_vsync = vsync
        m_cvmaj = cvmaj
        m_cvmin = cvmin
        m_title = title
        m_msaasamples = msaasamples
        m_updatefrequency = updatefrequency

        check(GLFW.glfwInit()) { "GLFW initialization failed." }

        GLFW.glfwSetErrorCallback { _, description ->
            val msg = MemoryUtil.memUTF8(description)
            println(msg)
        }

        GLFW.glfwDefaultWindowHints()
        if (m_msaasamples > 0) GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, m_msaasamples)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, m_cvmaj)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, m_cvmin)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE)

        m_window = GLFW.glfwCreateWindow(windowWidth, windowHeight, m_title, if (m_fullscreen) GLFW.glfwGetPrimaryMonitor() else 0L, 0)
        check(m_window != 0L) { "GLFW window couldn't be created." }

        initializeCallbacks()

        GLFW.glfwMakeContextCurrent(m_window)
        GLFW.glfwSwapInterval(if (m_vsync) 1 else 0)
        GLFW.glfwShowWindow(m_window)

        m_caps = GL.createCapabilities(true)
        if (m_msaasamples > 0) GL11.glEnable(GL13.GL_MULTISAMPLE)
    }

    private fun initializeCallbacks() {
        GLFW.glfwSetErrorCallback { _, description ->
            println("OpenGL Error: " + GLFWErrorCallback.getDescription(description))
        }

        GLFW.glfwSetKeyCallback(m_window) { _, key, scancode, action, mods ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) quit()
            onKey(key, scancode, action, mods)
        }

        GLFW.glfwSetMouseButtonCallback(m_window) { _, button, action, mods ->
            onMouseButton(button, action, mods)
        }

        GLFW.glfwSetCursorPosCallback(m_window) { _, xpos, ypos ->
            onMouseMove(xpos, ypos)
        }

        GLFW.glfwSetScrollCallback(m_window) { _, xoffset, yoffset ->
            onMouseScroll(xoffset, yoffset)
        }

        GLFW.glfwSetFramebufferSizeCallback(m_window) { _, width, height ->
            framebufferWidth = width
            framebufferHeight = height
            GL11.glViewport(0, 0, width, height)
            onFrameBufferSize(width, height)
        }

        GLFW.glfwSetWindowSizeCallback(m_window) { _, width, height ->
            windowWidth = width
            windowHeight = height
            onWindowSize(width, height)
        }
    }

    //Methods to be called by child classes
    /**
     * Tells the application to quit.
     * shutdown() is called after the last simulation step has completed.
     */
    protected fun quit() {
        GLFW.glfwSetWindowShouldClose(m_window, true)
    }

    /**
     * Returns the current mouse position in screen coordinates
     * @return Current mouse position
     */
    val mousePos: MousePosition
        get() {
            val x = BufferUtils.createDoubleBuffer(1)
            val y = BufferUtils.createDoubleBuffer(1)
            GLFW.glfwGetCursorPos(m_window, x, y)
            return MousePosition(x[0], y[0])
        }

    /**
     * Queries the state of a given key
     * @param key The GLFW key name
     * @return false, if the key is released; true, if the key is pressed
     */
    fun getKeyState(key: Int): Boolean {
        return GLFW.glfwGetKey(m_window, key) == GLFW.GLFW_PRESS
    }

    /**
     * Toggles cursor capture mode
     * @param visible if false, the cursor becomes invisible and is captured by the window.
     */
    fun setCursorVisible(visible: Boolean) {
        GLFW.glfwSetInputMode(m_window, GLFW.GLFW_CURSOR, if (visible) GLFW.GLFW_CURSOR_NORMAL else GLFW.GLFW_CURSOR_DISABLED)
    }

    val currentTime: Float
        get() = (m_currentTime * 1e-9).toFloat()

    //Methods to override by child classes
    /**
     * Is called once when the application starts
     */
    protected open fun start() {}

    /**
     * is called when the application quits
     */
    protected open fun shutdown() {}

    /**
     * Is called for every game state update.
     * The method is called in fixed time steps if possible.
     * Make sure that one update call takes no longer than 1/updatefrequency seconds, otherwise the
     * game slows down.
     *
     * This method should be used for physics simulations, where explicit solvers need small and constant
     * time steps to stay stable.
     *
     * @param dt Time delta to advance the game state simulation. dt is 1/updatefrequency seconds, constant.
     */
    protected open fun update(dt: Float, t: Float) {}

    /**
     * Is called for each frame to be rendered.
     *
     * @param dt Time in seconds the last frame needed to complete
     */
    protected open fun render(dt: Float, t: Float) {}

    /**
     * Is called when a mouse move event occurs
     * @param xpos  screen coordinate x value
     * @param ypos  screen coordinate y value
     */
    protected open fun onMouseMove(xpos: Double, ypos: Double) {}

    /**
     * Is called when a mouse button is pressed or released
     * @param button    GLFW mouse button name
     * @param action    GLFW action name
     * @param mode      GLFW modifiers
     */
    protected fun onMouseButton(button: Int, action: Int, mode: Int) {}

    /**
     * Is called when a scroll event occurs
     * @param xoffset   x offset of the mouse wheel
     * @param yoffset   y offset of the mouse wheel
     */
    protected fun onMouseScroll(xoffset: Double, yoffset: Double) {}

    /**
     * Is called when a key is pressed or released
     * @param key       GLFW key name
     * @param scancode  scancode of the key
     * @param action    GLFW action name
     * @param mode      GLFW modifiers
     */
    protected open fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    /**
     * Is called when the default frame buffer size changes (i.e. through resizing the window)
     * @param width     new frame buffer width
     * @param height    new frame buffer height
     */
    protected fun onFrameBufferSize(width: Int, height: Int) {
        framebufferWidth = width
        framebufferHeight = height
    }

    /**
     * Is called when the window size changes
     * @param width     new window width
     * @param height    new window height
     */
    protected fun onWindowSize(width: Int, height: Int) {
        windowWidth = width
        windowHeight = height
    }
    //public methods
    /**
     * Enters the game loop and loops until an error occurs or quit() is called
     */
    fun run() {
        start()

        val timedelta = (1.0 / m_updatefrequency * 1000000000.0).toLong()
        var currenttime: Long = 0
        var frametime: Long = 0
        var newtime: Long = 0
        var accum: Long = 0
        m_currentTime = 0
        currenttime = System.nanoTime()
        while (!GLFW.glfwWindowShouldClose(m_window)) {
            newtime = System.nanoTime()
            frametime = newtime - currenttime
            m_currentTime += frametime
            currenttime = newtime
            accum += frametime
            GLFW.glfwPollEvents()
            while (accum >= timedelta) {
                update((timedelta.toDouble() * 1e-9).toFloat(), (m_currentTime * 1e-9).toFloat())
                accum -= timedelta
            }
            render((frametime.toDouble() * 1e-9).toFloat(), (m_currentTime * 1e-9).toFloat())
            GLFW.glfwSwapBuffers(m_window)
        }
        shutdown()
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(m_window)
        GLFW.glfwDestroyWindow(m_window)
        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()
    }
}