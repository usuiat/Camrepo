package net.engawapg.app.camrepo

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import net.engawapg.app.camrepo.note.NoteViewModel
import net.engawapg.app.camrepo.notelist.NoteListViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private val noteListViewModel: NoteListViewModel by viewModel()
    private val noteViewModel: NoteViewModel by viewModel()
    private var displayAfterCloseDrawer: Int = DISPLAY_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Navigation setup */
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph, drawer_layout)

        /* Toolbar */
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        /* Navigation drawer */
        nav_view.setupWithNavController(navController)
        /* ノートが選択されたら画面を閉じる */
        noteListViewModel.selectedNote.observe(this, Observer{ note ->
            if (note != null) {
                drawer_layout.close()
                displayAfterCloseDrawer = DISPLAY_NOTE /* Drawer閉じ終わったらノート表示更新 */
            } else {
                noteViewModel.setNote(null)
            }
        })
        /* Drawer表示状態変更時の処理 */
        drawer_layout.addDrawerListener(object: DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                if (displayAfterCloseDrawer == DISPLAY_NOTE) {
                    displayAfterCloseDrawer = DISPLAY_NONE
                    noteViewModel.setNote(noteListViewModel.selectedNote.value)
                }
            }
        })


    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val TAG = "MainActivity"

        /* 表示内容 */
        private const val DISPLAY_NONE = 0
        private const val DISPLAY_NOTE = 1
    }
}