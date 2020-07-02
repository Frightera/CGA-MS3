package cga.exercise.components.camera

import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f

interface ICamera {

    /*
     * Calculate the ViewMatrix according the lecture
     * values needed:
     *  - eye –> the position of the camera
     *  - center –> the point in space to look at
     *  - up –> the direction of 'up'
     */
    fun getCalculateViewMatrix(): Matrix4f

    /*
     * Calculate the ProjectionMatrix according the lecture
     * values needed:
     *  - fov – the vertical field of view in radians (must be greater than zero and less than PI)
     *  - aspect – the aspect ratio (i.e. width / height; must be greater than zero)
     *  - zNear – near clipping plane distance
     *  - zFar – far clipping plane distance
     */
    fun getCalculateProjectionMatrix(): Matrix4f

    fun bind(shader: ShaderProgram)

}