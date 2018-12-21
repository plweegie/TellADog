package com.plweegie.android.telladog.camera

import android.util.Size
import kotlin.math.sign


class SizeAreaComparator : Comparator<Size> {

    override fun compare(lhs: Size,rhs: Size): Int =
            (lhs.width.toLong() * lhs.height - (rhs.width.toLong() * rhs.height)).sign
}