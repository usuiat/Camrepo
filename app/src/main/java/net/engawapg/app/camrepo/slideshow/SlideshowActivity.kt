package net.engawapg.app.camrepo.slideshow

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activity_slideshow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.viewModel

class SlideshowActivity : AppCompatActivity() {

    private val viewModel: SlideshowViewModel by viewModel()
    private var isFullScreen = false
    private lateinit var hideSystemUiJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slideshow)

        /* 画面の切り欠きがあるエリアにはレイアウトしない */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
        }

        /* ウィンドウ領域の変更を検知し、StatusBarの下端にToolBarの上端を合わせる。*/
        fullScreenContent.setOnApplyWindowInsetsListener { _, insets ->
            Log.d(TAG, "System Inset Top ${insets.systemWindowInsetTop}")
            val mlp = toolbar.layoutParams as ViewGroup.MarginLayoutParams
            mlp.setMargins(0, insets.systemWindowInsetTop, 0, 0)
            toolbar.layoutParams = mlp
            insets
        }

        /* ToolBarはSystemUIの表示状態に合わせる */
        /* タップ検出リスナーで処理してしまうと、タップ以外でSystemUI状態が変わった場合を見逃してしまう */
        fullScreenContent.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                toolbar.visibility = View.VISIBLE
                uiBackGround.visibility = View.VISIBLE
            } else {
                toolbar.visibility = View.INVISIBLE
                uiBackGround.visibility = View.INVISIBLE
            }
        }

        showSystemUI() /* LAYOUT系のフラグを最初に設定しておく必要がある */
        /* ちょっと待ってから全画面にする */
        hideSystemUiJob = lifecycleScope.launch {
            delay(2000)
            isFullScreen = true
            hideSystemUI()
        }

        /* Get page index */
        val pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* Pager */
        slidePager.apply {
            offscreenPageLimit = 1
            adapter = SlideAdapter(this@SlideshowActivity, viewModel)
            setCurrentItem(pageIndex, false)
        }

        /* タップ（だけ）を検出し、StatusBarなどの表示・非表示を切り替える */
        rootLayout.setSingleTapListener(object: TouchDetectLayout.SingleTapListener {
            override fun onSingleTap() {
                hideSystemUiJob.cancel() /* 最初の自動全画面化処理のキャンセル */
                toggleSystemUI()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleSystemUI() {
        isFullScreen = !isFullScreen
        if (isFullScreen) {
            hideSystemUI()
        } else {
            showSystemUI()
        }
    }

    private fun hideSystemUI() {
        Log.d(TAG, "Hide System UI")
        fullScreenContent.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        Log.d(TAG, "Show System UI")
        fullScreenContent.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }


    class SlideAdapter(fa: FragmentActivity, private val viewModel: SlideshowViewModel)
        : FragmentStateAdapter(fa) {
        override fun getItemCount() = viewModel.getSlideCount()
        override fun createFragment(position: Int): Fragment {
            return SlideshowFragment.newInstance(position)
        }
    }

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
        private const val TAG = "SlideshowActivity"
    }
}

/**
 * タップを検出するためのLayout。
 * 子View（ここではPager）でスクロールされた場合は何もせず、シングルタップだけを検出する。
 */
class TouchDetectLayout(context: Context, attrs: AttributeSet): ConstraintLayout(context, attrs) {

    interface SingleTapListener {
        fun onSingleTap()
    }

    private var singleTapListener: SingleTapListener? = null

    fun setSingleTapListener(listener: SingleTapListener) {
        singleTapListener = listener
    }

    /*
     * onInterceptTouchEventではfalseを返し、通常通り子Viewによってタッチイベントが処理されるようにする。
     * 子ViewがOnClickを処理した場合はACTION_UPが来るので、シングルタップだと判定できる。
     * ドラッグした場合はOnClickが発動しないので、ACTION_UPが来ない。
     * 全面でタップを検出するため、このレイアウトのすぐ上にダミーのclickableなViewをかぶせる。
     */
    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val action = event.actionMasked
            Log.d(TAG, "onInterceptTouchEvent ${MotionEvent.actionToString(action)}")
            if (action == MotionEvent.ACTION_UP) {
                singleTapListener?.onSingleTap()
            }
        }
        return false
    }

    companion object {
        private const val TAG = "TouchDetectLayout"
    }
}