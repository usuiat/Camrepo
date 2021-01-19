package net.engawapg.app.camrepo

import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.page.CameraViewModel
import net.engawapg.app.camrepo.note.NoteViewModel
import net.engawapg.app.camrepo.notelist.EditTitleViewModel
import net.engawapg.app.camrepo.notelist.NoteListViewModel
import net.engawapg.app.camrepo.page.PageViewModel
import net.engawapg.app.camrepo.photo.PhotoViewModel
import net.engawapg.app.camrepo.slideshow.SlideViewModel
import net.engawapg.app.camrepo.slideshow.SlideshowViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmField
val appModule = module {

    scope(named(Constants.SCOPE_NAME_NOTE)) {
        scoped { NoteModel(androidApplication()) }
    }

    single { NoteListModel(androidApplication()) }

    viewModel { (pageIndex: Int, columnCount: Int) ->
        PageViewModel(
            getScope(Constants.SCOPE_ID_NOTE).get(),
            get(),
            pageIndex,
            columnCount
        )
    }

    viewModel {
        CameraViewModel(
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel {
        PhotoViewModel(
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel { (noteFileName: String) ->
        NoteViewModel(noteFileName, get())
    }

    viewModel { NoteListViewModel(get()) }

    viewModel {
        SlideshowViewModel(
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel {
        SlideViewModel(
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel {
        EditTitleViewModel(getScope(Constants.SCOPE_ID_NOTE).get(), get())
    }
}