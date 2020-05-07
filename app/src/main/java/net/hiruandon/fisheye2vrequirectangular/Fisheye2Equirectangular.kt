package net.hiruandon.fisheye2vrequirectangular

import android.graphics.PointF

class Fisheye2Equirectangular {
    private fun findCorrespondingFisheyePoint(Xe: Int, Ye: Int, We: Int, He: Int, FOV: Float):PointF{
        var fisheyePoint = PointF()
        var theta = Math.PI * (Xe / We.toFloat() - 0.5)
        var phi = Math.PI * (Ye / He.toFloat() - 0.5)

        val sphericalPointX = Math.cos(phi) * Math.sin(theta)
        val sphericalPointY = Math.cos(phi) * Math.cos(theta)
        val sphericalPointZ = Math.sin(phi);

        theta = Math.atan2(sphericalPointZ, sphericalPointX)
        phi = Math.atan2(Math.sqrt(Math.pow(sphericalPointX, 2.0) + Math.pow(sphericalPointZ, 2.0)), sphericalPointY)
        val r = We * phi / FOV;

        fisheyePoint.x = (0.5 * We + r * Math.cos(theta)).toFloat();
        fisheyePoint.y = (0.5 * He + r * Math.sin(theta)).toFloat();

        return fisheyePoint
    }
}