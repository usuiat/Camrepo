package net.engawapg.app.camrepo.notelist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.engawapg.app.camrepo.R

class NoteListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, NoteListFragment.newInstance())
                .commitNow()
        }
    }
}