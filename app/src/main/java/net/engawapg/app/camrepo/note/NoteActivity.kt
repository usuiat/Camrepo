package net.engawapg.app.camrepo.note

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.engawapg.app.camrepo.R

class NoteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NoteFragment.newInstance())
                .commitNow()
        }
    }

}