package cga.exercise.components.geometry

import org.joml.Matrix4f
import org.joml.Vector3f
import javax.swing.text.MutableAttributeSet

open class Transformable : ITransformable {

    var modelmatrix = Matrix4f()
    var parent :Transformable? = null

    /**
     * Rotates object around its own origin.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     */
    override fun rotateLocal(pitch: Float, yaw: Float, roll: Float){

        modelmatrix.rotateXYZ(pitch, yaw,roll)
    }

    /**
     * Rotates object around given rotation center.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     * @param altMidpoint rotation center
     */
    override fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f){

        var matrix = Matrix4f()
        matrix.translate(altMidpoint)
        matrix.rotateXYZ(pitch,yaw,roll)
        matrix.translate(Vector3f(altMidpoint).negate())
        modelmatrix = matrix.mul(modelmatrix)
    }

    /**
     * Translates object based on its own coordinate system.
     * @param deltaPos delta positions
     */
    override fun translateLocal(deltaPos: Vector3f){
        modelmatrix.translate(deltaPos)
    }

    /**
     * Translates object based on its parent coordinate system.
     * Hint: global operations will be left-multiplied
     * @param deltaPos delta positions (x, y, z)
     */
    override fun translateGlobal(deltaPos: Vector3f){
        var x = Matrix4f()
        x.translate(deltaPos)
        modelmatrix = modelmatrix.mul(x)

    }

    /**
     * Scales object related to its own origin
     * @param scale scale factor (x, y, z)
     */
    override fun scaleLocal(scale: Vector3f){
        modelmatrix.scale(scale)
    }

    /**
     * Returns position based on aggregated translations.
     * Hint: last column of model matrix
     * @return position
     */
    override fun getPosition(): Vector3f{
        var x : Vector3f =Vector3f(modelmatrix.m30(),modelmatrix.m31(),modelmatrix.m32())
        return x
    }

    /**
     * Returns position based on aggregated translations incl. parents.
     * Hint: last column of world model matrix
     * @return position
     */
    override fun getWorldPosition(): Vector3f{
        var world = getWorldModelMatrix()
        var position = Vector3f(world.m30(),world.m31(),world.m32())
        return position
    }

    /**
     * Returns x-axis of object coordinate system
     * Hint: first normalized column of model matrix
     * @return x-axis
     */
    override fun getXAxis(): Vector3f{
        var x_axis = Vector3f(modelmatrix.m00(),modelmatrix.m01(),modelmatrix.m02()).normalize()
        return x_axis
    }

    /**
     * Returns y-axis of object coordinate system
     * Hint: second normalized column of model matrix
     * @return y-axis
     */
    override fun getYAxis(): Vector3f{
        return Vector3f(modelmatrix.m10(),modelmatrix.m11(),modelmatrix.m12()).normalize()
    }

    /**
     * Returns z-axis of object coordinate system
     * Hint: third normalized column of model matrix
     * @return z-axis
     */
    override fun getZAxis(): Vector3f{
        return Vector3f(modelmatrix.m20(),modelmatrix.m21(),modelmatrix.m22()).normalize()
    }

    /**
     * Returns x-axis of world coordinate system
     * Hint: first normalized column of world model matrix
     * @return x-axis
     */
    override fun getWorldXAxis(): Vector3f{
        var world = getWorldModelMatrix()
        return Vector3f(world.m00(),world.m01(),world.m02()).normalize()
    }

    /**
     * Returns y-axis of world coordinate system
     * Hint: second normalized column of world model matrix
     * @return y-axis
     */
    override fun getWorldYAxis(): Vector3f{
        var world = getWorldModelMatrix()
        return Vector3f(world.m10(),world.m11(),world.m12()).normalize()
    }

    /**
     * Returns z-axis of world coordinate system
     * Hint: third normalized column of world model matrix
     * @return z-axis
     */
    override fun getWorldZAxis(): Vector3f{
        var world = getWorldModelMatrix()
        return Vector3f(world.m20(),world.m21(),world.m22()).normalize()
    }


    /**
     * Returns multiplication of world and object model matrices.
     * Multiplication has to be recursive for all parents.
     * Hint: scene graph
     * @return world modelMatrix
     */
    override fun getWorldModelMatrix(): Matrix4f{
        var local = getLocalModelMatrix()
        parent?.getWorldModelMatrix()?.mul(getLocalModelMatrix(),local)
        return local
    }

    /**
     * Returns object model matrix
     * @return modelMatrix
     */
    override fun getLocalModelMatrix(): Matrix4f{
        return Matrix4f(modelmatrix)
    }



}