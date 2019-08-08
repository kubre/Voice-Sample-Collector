package `in`.foreplus.voicesample


import `in`.foreplus.voicesample.animations.ReverseInterpolator
import `in`.foreplus.voicesample.utils.FileUtil
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.android.synthetic.main.activity_record.*
import omrecorder.*
import java.io.File


const val RECORD_PERMISSION_CODE = 30

class RecordActivity : AppCompatActivity() {
    private val samplingFrequencies: Array<String> = arrayOf(
        "8000", "11025", "12000", "16000", "22050", "24000", "32000", "44100", "48000", "64000", "96000", "192000"
    )
    private var isRecording = false
    private var canPlay = false
    private var recorder: Recorder? = null
    private var audioPlayer: MediaPlayer? = null
    private var filename: File? = null

    private var dataName: String? = null
    private var dataSet: List<String>? = null
    private var dataCursor: Int = 0

    private lateinit var getProfileNameDialog: AlertDialog
    private lateinit var getSamplingFrequencyDialog: AlertDialog

    private var recordingChannel = AudioFormat.CHANNEL_IN_MONO
    private var recordingFrequency: Int = samplingFrequencies[0].toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        // region { Initialization }
        val dataFile = File(externalMediaDirs[0], DATA_FILE)
        if (!hasRecordPermission()) {
            ActivityCompat.requestPermissions(
                this@RecordActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_PERMISSION_CODE
            )
        }
        if (dataFile.exists() && dataFile.canRead()) {
            val data = FileUtil.decodeData(dataFile)
            dataName = data.first
            dataSet = data.second
            txtDataFileName.text = dataName
            resetProfile()
            setupDialogBoxes()
            setUpRecording()
        } else {
            MaterialAlertDialogBuilder(this@RecordActivity).apply {
                title = "No Data Found"
                setMessage("Please upload data file before you continue!")
                setNeutralButton("OK") { d, _ ->
                    d.dismiss()
                    val mainScreen = Intent(this@RecordActivity, MainActivity::class.java)
                    startActivity(mainScreen)
                }
            }
        }
        // endregion
    }

    private fun setUpRecording() {
        when (recordingChannel) {
            R.id.rdbMono -> rdbMono.isSelected = true
            R.id.rdbStereo -> rdbStereo.isSelected = true
            else -> rdbMono.isSelected = true
        }
        val recordButtonAnimation = TransitionDrawable(
            arrayOf(
                getDrawable(R.drawable.blue_circle_shape),
                getDrawable(R.drawable.red_circle_shape)
            )
        )
        val scaleUpAnim = AnimationUtils.loadAnimation(this, R.anim.record_button)
        val scaleDownAnim = AnimationUtils.loadAnimation(this, R.anim.record_button)
        scaleDownAnim.interpolator = ReverseInterpolator()

        btnRecord.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (dataCursor > dataSet!!.size - 1) {
                return@setOnTouchListener false
            }
            Log.d("COUNT", "$dataCursor")
            val folder = File("${externalMediaDirs[0].absoluteFile}/$dataName/${txtProfileName.text}")
            filename =
                File(
                    folder,
                    "${dataSet?.get(dataCursor)}.wav"
                )
            if (!folder.exists()) {
                folder.mkdirs()
            }
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (hasRecordPermission()) {
                        startRecording()
                        v?.startAnimation(scaleUpAnim)
                        v?.background = recordButtonAnimation
                        recordButtonAnimation.startTransition(200)
                    } else {
                        ActivityCompat.requestPermissions(
                            this@RecordActivity,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            RECORD_PERMISSION_CODE
                        )
                    }
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    v?.startAnimation(scaleDownAnim)
                    v?.background = recordButtonAnimation
                    recordButtonAnimation.reverseTransition(200)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        recorderAmplitudeBar.setProgress(0, true)
                    } else {
                        animateAmplitudeBar(0)
                    }
                }
            }
            false
        }

        btnNext.setOnClickListener {
            if (canPlay) {
                nextSampleToRecord()
            }
        }

        btnPlay.setOnClickListener {
            if (canPlay) {
                try {
                    audioPlayer = MediaPlayer().apply {
                        btnPlay.setImageDrawable(getDrawable(R.drawable.ic_pause))
                        setDataSource(filename?.absolutePath)
                        prepare()
                        start()
                        setOnCompletionListener {
                            btnPlay.setImageDrawable(getDrawable(R.drawable.ic_play))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun nextSampleToRecord() {
        canPlay = false
        dataCursor++
        if (dataCursor < dataSet!!.size) {
            txtToRecord.text = dataSet?.get(dataCursor)
        } else {
            txtToRecord.text = "Finished"
            txtToRecord.setTextColor(Color.rgb(235, 98, 98))
        }
    }

    private fun setupDialogBoxes() {
        getProfileNameDialog = MaterialAlertDialogBuilder(this@RecordActivity).apply {
            setTitle("Set Profile Name")
            val input = EditText(this@RecordActivity)
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.setTextColor(Color.WHITE)
            setView(input)
            setPositiveButton("OK") { d, _ ->
                txtProfileName.text = input.text.toString()
                resetProfile()
                d.dismiss()
            }
            setNegativeButton("Cancel") { d, _ -> d.cancel() }
        }.create()

        getSamplingFrequencyDialog = MaterialAlertDialogBuilder(this@RecordActivity).apply {
            setTitle("Set Sampling Frequency")
            setItems(samplingFrequencies) { d, which ->
                recordingFrequency = samplingFrequencies[which].toInt()
                txtSelectedFrequency.text = "$recordingFrequency Hz"
                d.dismiss()
            }
            setNegativeButton("Cancel") { d, _ -> d.cancel() }
        }.create()

        btnChangeFrequency.setOnClickListener {
            getSamplingFrequencyDialog.show()
        }
        btnEditProfile.setOnClickListener {
            getProfileNameDialog.show()
        }
    }

    private fun resetProfile() {
        txtToRecord.setTextColor(Color.WHITE)
        dataCursor = -1
        nextSampleToRecord()
    }

    fun onSelectChannel(view: View) {
        if (view is MaterialRadioButton) {
            if (view.isChecked) {
                recordingChannel = when (view.id) {
                    R.id.rdbMono -> AudioFormat.CHANNEL_IN_MONO
                    R.id.rdbStereo -> AudioFormat.CHANNEL_IN_STEREO
                    else -> AudioFormat.CHANNEL_IN_MONO
                }
            }
        }
        Log.d("APP_TAG", "$recordingChannel")
    }

    // region { Recording methods }
    private fun startRecording() {
        if (!isRecording) {
            Toast.makeText(this@RecordActivity, "Recording...", Toast.LENGTH_LONG).show()
            Log.d("APP_TAG", filename?.absolutePath)
            isRecording = true
            canPlay = false
            recorder = OmRecorder.wav(
                PullTransport.Default(
                    sourceMic(),
                    PullTransport.OnAudioChunkPulledListener { audioChunk ->
                        animateAmplitudeBar(audioChunk.maxAmplitude().toInt())
                    }),
                filename
            )
            recorder?.startRecording()
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            Toast.makeText(this@RecordActivity, "Stop", Toast.LENGTH_LONG).show()
            recorder?.stopRecording()
            isRecording = false
            canPlay = true
        }
    }

    private fun sourceMic(): PullableSource.Default {
        return PullableSource.Default(
            AudioRecordConfig.Default(
                MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                recordingChannel, recordingFrequency
            )
        )
    }

    private fun animateAmplitudeBar(amplitude: Int) {
        recorderAmplitudeBar.progress = amplitude
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this@RecordActivity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    // endregion
}
