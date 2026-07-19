package org.akanework.gramophone.logic.utils.exoplayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import org.akanework.gramophone.logic.getBooleanStrict
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener

@OptIn(UnstableApi::class)
class GramophoneRenderFactory(context: Context) : DefaultRenderersFactory(context) {
	override fun buildAudioSink(
		context: Context,
		enableFloatOutput: Boolean,
		enableAudioTrackPlaybackParams: Boolean
	): AudioSink? {
		val prefs = PreferenceManager.getDefaultSharedPreferences(context)
		val enableDacBypass = prefs.getBooleanStrict("dac_bypass", false)
		if (enableDacBypass) {
			val sink = DefaultAudioSink.Builder(context)
				.setEnableFloatOutput(enableFloatOutput)
				.setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
				.build()
			sink.setOffloadMode(DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED)
			return sink
		}
		return super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
	}

	override fun buildTextRenderers(
		context: Context,
		output: TextOutput,
		outputLooper: Looper,
		extensionRendererMode: Int,
		out: ArrayList<Renderer>
	) {
		// empty
	}

	override fun buildVideoRenderers(
		context: Context,
		extensionRendererMode: Int,
		mediaCodecSelector: MediaCodecSelector,
		enableDecoderFallback: Boolean,
		eventHandler: Handler,
		eventListener: VideoRendererEventListener,
		allowedVideoJoiningTimeMs: Long,
		out: java.util.ArrayList<Renderer>
	) {
		// empty
	}

	override fun buildImageRenderers(out: java.util.ArrayList<Renderer>) {
		// empty
	}

	override fun buildCameraMotionRenderers(
		context: Context,
		extensionRendererMode: Int,
		out: java.util.ArrayList<Renderer>
	) {
		// empty
	}
}