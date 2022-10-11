package me.relex.widget.waveform

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Handler
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Copyright (C), 2021-2099
 *
 * @author Cooper
 * History:
 * author - date - version - desc
 * Cooper 2022/9/27 15:31 1  简述该类的作用等
 */
class WaveFormInfo private constructor(
    val sampleRate: Int, //采样率
    val channel: Int,    //通道，应该是2，不知道多个或者1个会是啥情况
    val duration: Long,  //持续时间，毫秒。其他地方统一用的double类型的秒计算。参与显示的是计算出来的秒，不是这个值。
    var samples: ShortArray = ShortArray(0) //数据，原始数据，会进行一次重新采样。不用这个内容。
) {
    var samplePerPixel = 436.92 //反向计算出来的，不然的话不契合，导致右边有误差。

    private fun getTotalTime(): Long {
        return WaveUtil.dataPixelsToTime(
            getLength(),
            getSample_rate(),
            getSamples_per_pixel()
        )
    }

    //计算每像素显示多少，不然可能右边有误差。
    private fun calcSamplesPerPixel() {
        //反向计算 getSamples_per_pixel； dataPixelsToSecond（）
        //会有些误差，不过不要紧。
        samplePerPixel = duration / 1000 * sampleRate / getLength().toDouble()
    }

    /**
     * 获取每毫秒的采样数
     * @return Double
     */
    public fun getSamplePerMs(): Double {
        return resampleData.size / getTotalTime().toDouble();
    }

    /**
     * 获取指定位置的像素数据
     * @param pos Int 位置，数组位置，根据距离计算出来的
     * @return Float 数据
     */
    public fun getPixelData(pos: Int): Float {
        return resampleData.get(pos)
    }

    /**
     * 获取数据长度
     * @return Int
     */
    public fun getLength(): Int {
        return resampleData.size
    }

    /**
     * 获取采样率
     * @return Int
     */
    public fun getSample_rate(): Int {
        return sampleRate
    }

    /**
     * 每像素的采样率，就是每像素多少数据吧。
     * @return Int
     */
    public fun getSamples_per_pixel(): Int {
        return samplePerPixel.toInt()
    }

    /**
     * 取平均值？不知道是什么意思
     * @param start Int
     * @param end Int
     * @return Float
     */
    public fun average(start: Int, end: Int): Float {
        var sum = 0.0
        for (i in start until end) {
            sum += Math.abs(samples[i].toDouble())
        }

        return sum.toFloat() / (end - start)
    }

    //重新采样后的数据，用来显示的数据
    private var resampleData = FloatArray(0)

    //准备用来显示的数据。不懂什么个算法。
    private fun prepareData() {
        val barPerSample: Int = (0.01f * sampleRate * channel).toInt()
        val count: Int = samples.size / barPerSample
        resampleData = FloatArray(count)
        for (i in 0 until count) {
            resampleData[i] = average(i * barPerSample, (i + 1) * barPerSample)
        }
        calcSamplesPerPixel()
    }

    private constructor(
        sampleRate: Int,
        channel: Int,
        duration: Long,
        stream: ByteArrayOutputStream
    ) : this(sampleRate, channel, duration) {
        samples = stream.toShortArray()
        prepareData()
    }

    public fun setSamples(stream: ByteArrayOutputStream) {
        samples = stream.toShortArray()
        prepareData()
    }

    private fun ByteArrayOutputStream.toShortArray(): ShortArray {
        val array =
            ByteBuffer.wrap(this.toByteArray()).order(ByteOrder.nativeOrder()).asShortBuffer()
        val results = ShortArray(array.remaining())
        array.get(results)
        return results
    }

    /**
     * Factory class to build [WaveFormInfo]
     *
     * Note : It build data asynchronously
     */
    class Factory {

        /**
         *Provide callbacks indicating progress to the user.
         */
        interface Callback {
            /**
             * 开始处理数据，此时还没有 samples。
             * @param data WaveFormInfo 里面的samples此时是空的。
             */
            fun onExtraStart(data: WaveFormInfo)

            /**
             * Called when has progress
             * You can indicate progress to user using ProgressBar
             * 本来想动态展示解压过程的，但是貌似比较麻烦，先这样吧。
             * @param data WaveFormInfo
             * @param progress value indicating progress in 0-100
             * @param newData ByteArray 本次解析的新数据。
             */
            fun onExtraProgress(data: WaveFormInfo, progress: Float, newData: ByteArray)

            /**
             * Called when complete
             * Take out your data built
             *@param waveFormData built data
             */
            fun onExtraComplete(data: WaveFormInfo)

            /**
             * 处理失败
             * @param e Exception
             */
            fun onExtraFailed(e: Exception)
        }

        private val extractor = MediaExtractor()
        private var audioTrackIndex = -1

        private constructor()

        /**
         * Sets the data source (AssetFileDescriptor) to use.
         * It is the caller's responsibility to close the file descriptor.
         * It is safe to do so as soon as this call returns.
         * @param afd the AssetFileDescriptor for the file you want to extract from.
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        @RequiresApi(24)
        constructor(afd: AssetFileDescriptor) {
            extractor.setDataSource(afd)
            init()
        }

        /**
         * Sets the data source as a content Uri.
         * @param context the Context to use when resolving the Uri
         * @param uri the Content URI of the data you want to extract from.
         * @param headers the headers to be sent together with the request for the data. This can be null if no specific headers are to be sent with the request.
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        constructor(context: Context, uri: Uri, headers: Map<String, String>) {
            extractor.setDataSource(context, uri, headers)
            init()
        }

        /**
         * Sets the data source (FileDescriptor) to use.
         * It is the caller's responsibility to close the file descriptor.
         * It is safe to do so as soon as this call returns.
         * @param fd the FileDescriptor for the file you want to extract from.
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        constructor(fd: FileDescriptor) {
            extractor.setDataSource(fd)
            init()
        }

        /**
         * Sets the data source (MediaDataSource) to use.
         * @param dataSource the MediaDataSource for the media you want to extract from
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        @RequiresApi(23)
        constructor(dataSource: MediaDataSource) {
            extractor.setDataSource(dataSource)
            init()
        }

        /**
         * Sets the data source (FileDescriptor) to use.
         * The FileDescriptor must be seekable (N.B. a LocalSocket is not seekable).
         * It is the caller's responsibility to close the file descriptor.
         * It is safe to do so as soon as this call returns.
         * @param fd the FileDescriptor for the file you want to extract from.
         * @param offset  the offset into the file where the data to be extracted starts, in bytes
         * @param length the length in bytes of the data to be extracted
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        constructor(fd: FileDescriptor, offset: Long, length: Long) {
            extractor.setDataSource(fd, offset, length)
            init()
        }

        /**
         * Sets the data source (file-path or http URL) to use.
         * @param path the path of the file, or the http URL of the stream
         *When path refers to a local file, the file may actually be opened by a process other than the calling application.
         *This implies that the pathname should be an absolute path (as any other process runs with unspecified current working directory),
         *and that the pathname should reference a world-readable file.
         *As an alternative, the application could first open the file for reading,
         *and then use the file descriptor form setDataSource(FileDescriptor).
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        constructor(path: String) {
            extractor.setDataSource(path)
            init()
        }

        /**
         * Sets the data source (file-path or http URL) to use.
         * @param path the path of the file, or the http URL
         * When path refers to a network file the android.Manifest.permission.INTERNET permission is required.
         * @param headers the headers associated with the http request for the stream you want to play.
         * This can be null if no specific headers are to be sent with the request.
         * @throws Exception If media doesn't have audio track, throw exception.
         */
        constructor(path: String, headers: Map<String, String>) {
            extractor.setDataSource(path, headers)
            init()
        }

        private fun init() {
            if (extractor.trackCount == 0) throw Exception("No track")
            audioTrackIndex = extractor.getAudioTrackIndex()
            if (audioTrackIndex == -1) throw Exception("No audio track")
            extractor.selectTrack(audioTrackIndex)
        }

        private fun MediaExtractor.getAudioTrackIndex(): Int {
            for (i in 0 until extractor.trackCount) {
                // select audio track
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                        ?.contains("audio/") == true
                ) {
                    return i
                }
            }
            return -1
        }

        /**
         * Build a data using constructor params
         *
         * Note : It works asynchronously and takes several seconds
         *
         * @param callback callback to report progress and pass built data
         */
        fun build(callback: Callback) {
            try {
                val handler = Handler()
                val format = extractor.getTrackFormat(audioTrackIndex)
                val codec = MediaCodec.createDecoderByType(
                    format.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"
                )
                codec.configure(format, null, null, 0)
                val outFormat = codec.outputFormat

                val estimateSize =
                    format.getLong(MediaFormat.KEY_DURATION) / 1000000f * format.getInteger(
                        MediaFormat.KEY_CHANNEL_COUNT
                    ) * format.getInteger(
                        MediaFormat.KEY_SAMPLE_RATE
                    ) * 2f

                val startTime = System.currentTimeMillis()
                codec.start()
                Log.i("WaveFormFactory", "Start building data.")

                val data = WaveFormInfo(
                    outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    extractor.getTrackFormat(audioTrackIndex).getLong(MediaFormat.KEY_DURATION),
                )

                callback.onExtraStart(data)

                Thread {
                    try {
                        var EOS = false
                        val stream = ByteArrayOutputStream()
                        val onlyNewStream = ByteArrayOutputStream()
                        val info = MediaCodec.BufferInfo()

                        while (!EOS) {
                            val inputBufferId = codec.dequeueInputBuffer(10)
                            if (inputBufferId >= 0) {
                                codec.getInputBuffer(inputBufferId)?.let {
                                    val readSize = extractor.readSampleData(it, 0)
                                    extractor.advance()
                                    codec.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        if (readSize > 0) readSize else 0,
                                        extractor.sampleTime,
                                        if (readSize > 0) 0 else MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                }
                            }

                            val outputBufferId = codec.dequeueOutputBuffer(info, 10)
                            if (outputBufferId >= 0) {
                                codec.getOutputBuffer(outputBufferId)?.let {
                                    val buffer = ByteArray(it.remaining())
                                    it.get(buffer)
                                    onlyNewStream.write(buffer)
                                    callback.onExtraProgress(
                                        data,
                                        stream.size() / estimateSize * 100,
                                        onlyNewStream.toByteArray()
                                    )

                                    stream.write(buffer)
                                    onlyNewStream.reset()
                                }

                                codec.releaseOutputBuffer(outputBufferId, false)
                                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                    EOS = true
                                }
                            }
                        }

                        codec.stop()
                        codec.release()
                        extractor.release()

                        data.setSamples(stream)

                        Log.i(
                            "WaveFormFactory",
                            "Built data in " + (System.currentTimeMillis() - startTime) + "ms"
                        )

                        handler.post {
                            callback.onExtraComplete(data)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            callback.onExtraFailed(e)
                        }
                    }
                }.start()
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onExtraFailed(e)
            }
        }
    }

}


