package me.fexus.skeletalanimation

class Animation(val name: String, val keyFrames: List<KeyFrame>) {
    val firstKeyFrame: KeyFrame = keyFrames.minBy { it.timeStamp }
    val lastKeyFrame: KeyFrame = keyFrames.maxBy { it.timeStamp }
    val duration = lastKeyFrame.timeStamp - firstKeyFrame.timeStamp

    fun getPrevAndNextKeyFrame(animationTimeStamp: Float): HotKeyFrames {
        // not sure if < or <= is correct here...
        val previous = keyFrames.maxBy { if (it.timeStamp <= animationTimeStamp) it.timeStamp else -1f }
        val next = keyFrames.minBy { if (it.timeStamp > animationTimeStamp) it.timeStamp else Float.MAX_VALUE }
        return HotKeyFrames(previous, next)
    }
}