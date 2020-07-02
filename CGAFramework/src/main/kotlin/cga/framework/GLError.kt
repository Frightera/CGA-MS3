package cga.framework

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import kotlin.system.exitProcess

object GLError {

    fun checkEx(): Boolean {
        var errOccured = false
        waitForError(true) { ex, _ ->
            if (ex.isNotEmpty()) {
                System.err.println(ex.toString())
                errOccured = true
            }
        }
        return errOccured
    }

    fun checkThrow() {
        waitForError(false) { ex, _ ->
            if (ex.isNotEmpty()) {
                throw Exception(ex.toString())
            }
        }
    }

    fun checkExit() {
        waitForError(false) {ex, errorCode ->
            if (ex.isNotEmpty()) {
                System.err.println(ex)
                exitProcess(errorCode)
            }
        }
    }

    private fun waitForError(appendError: Boolean, whatToDoWithErrorSB: (sb: StringBuilder, errorCode: Int) -> Unit) {
        var errorCode: Int
        val ex = StringBuilder()
        while (GL11.glGetError().also { errorCode = it } != GL11.GL_NO_ERROR) {
            val error = errorCodeToString(errorCode)
            val stackElement = Thread.currentThread().stackTrace[3]
            val stuff = "An OpenGL error occured in File: " + stackElement.fileName + ", Line: " + stackElement.lineNumber
            ex.append(stuff)
            if(appendError) ex.append(", " + error)
            ex.append("\n")
        }
        whatToDoWithErrorSB(ex, errorCode)
    }

    private fun errorCodeToString(errorCode: Int): String {
        return when (errorCode) {
            GL11.GL_INVALID_ENUM -> "INVALID_ENUM"
            GL11.GL_INVALID_VALUE -> "INVALID_VALUE"
            GL11.GL_INVALID_OPERATION -> "INVALID_OPERATION"
            GL11.GL_STACK_OVERFLOW -> "STACK_OVERFLOW"
            GL11.GL_STACK_UNDERFLOW -> "STACK_UNDERFLOW"
            GL11.GL_OUT_OF_MEMORY -> "OUT_OF_MEMORY"
            GL30.GL_INVALID_FRAMEBUFFER_OPERATION -> "INVALID_FRAMEBUFFER_OPERATION"
            else -> ""
        }
    }
}