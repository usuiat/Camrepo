package net.engawapg.app.camrepo

import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.page.CameraViewModel
import net.engawapg.app.camrepo.note.NoteViewModel
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
            androidApplication(),
            getScope(Constants.SCOPE_ID_NOTE).get(),
            get(),
            pageIndex,
            columnCount
        )
    }

    viewModel {
        CameraViewModel(
            androidApplication(),
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel {
        PhotoViewModel(
            androidApplication(),
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel { NoteViewModel(androidApplication(), get()) }

    viewModel { NoteListViewModel(androidApplication(), get()) }

    viewModel {
        SlideshowViewModel(
            androidApplication(),
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }

    viewModel {
        SlideViewModel(
            androidApplication(),
            getScope(Constants.SCOPE_ID_NOTE).get()
        )
    }
}