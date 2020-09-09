package net.hiruandon.fisheye2vrequirectangular

import android.graphics.*

class Fisheye2Equirectangular {
    /**
     * 対応する座標を返す
     */
    private fun findCorrespondingFisheyePoint(Xe: Int, Ye: Int, We: Int, He: Int, FOV: Float):PointF{
        val fisheyePoint = PointF()
        var theta = Math.PI * (Xe / We.toFloat() - 0.5)
        var phi = Math.PI * (Ye / He.toFloat() - 0.5)

        val sphericalPointX = Math.cos(phi) * Math.sin(theta)
        val sphericalPointY = Math.cos(phi) * Math.cos(theta)
        val sphericalPointZ = Math.sin(phi)

        theta = Math.atan2(sphericalPointZ, sphericalPointX)
        phi = Math.atan2(Math.sqrt(Math.pow(sphericalPointX, 2.0) + Math.pow(sphericalPointZ, 2.0)), sphericalPointY)
        val r = We * phi / FOV

        fisheyePoint.x = (0.5 * We + r * Math.cos(theta)).toFloat()
        fisheyePoint.y = (0.5 * He + r * Math.sin(theta)).toFloat()

        return fisheyePoint
    }

    /**
     * 魚眼画像を正距円筒画像に変換する
     */
    fun fisheye2equirectangular(fisheyeImg:Bitmap, angle: Float):Bitmap {
        val len = if (fisheyeImg.width > fisheyeImg.height){ fisheyeImg.width } else { fisheyeImg.height }
        val baseImg = Bitmap.createBitmap(len,len,fisheyeImg.config)
        val canvas = Canvas(baseImg)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(fisheyeImg,(len-fisheyeImg.width)/2F,(len-fisheyeImg.height)/2F, null)
        // 正方形の回転画像取得完了

        val FOV = (angle * Math.PI)/180F

        val equiarray = IntArray(len*len){index: Int ->
            val p = findCorrespondingFisheyePoint(index % len, index / len,len,len,FOV.toFloat())
            baseImg.getPixel(p.x.toInt(),p.y.toInt())
        }

        return Bitmap.createBitmap(equiarray ,len,len,fisheyeImg.config)
    }
}