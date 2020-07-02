package cga.exercise.components.camera

import org.joml.Math
import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f

class TronCamera(var fieldOfView :Float = Math.toRadians(90f), var aspect : Float = 16f/9f,
                 var nearPlane : Float = 0.1f, var farPlane : Float = 100f  ): Transformable() , ICamera {


    /*
     * Calculate the ViewMatrix according the lecture
     * values needed:
     *  - eye –> the position of the camera
     *  - center –> the point in space to look at
     *  - up –> the direction of 'up'
     */
    override fun getCalculateViewMatrix(): Matrix4f{
        var viewMatirx = Matrix4f()
        return viewMatirx.lookAt(getWorldPosition(),getWorldPosition().sub(getWorldZAxis()),getWorldYAxis())
    }

    /*
     * Calculate the ProjectionMatrix according the lecture
     * values needed:
     *  - fov – the vertical field of view in radians (must be greater than zero and less than PI)
     *  - aspect – the aspect ratio (i.e. width / height; must be greater than zero)
     *  - zNear – near clipping plane distance
     *  - zFar – far clipping plane distance
     */
    override fun getCalculateProjectionMatrix(): Matrix4f{
        var projMatrix = Matrix4f()
        return projMatrix.perspective(fieldOfView,aspect,nearPlane,farPlane)
    }

    override fun bind(shader: ShaderProgram){
        shader.setUniform("view",getCalculateViewMatrix(),false)
        shader.setUniform("projection",getCalculateProjectionMatrix(),false)
    }
}