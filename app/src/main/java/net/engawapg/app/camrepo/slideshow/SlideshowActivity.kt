package net.engawapg.app.camrepo.slideshow

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.engawapg.app.camrepo.R

class SlideshowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slideshow)
    }

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
    }
}