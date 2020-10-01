package net.engawapg.app.camrepo

import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.note.NoteViewModel
import net.engawapg.app.camrepo.notelist.NoteListViewModel
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

//    viewModel { (pageIndex: Int) ->
//        PageViewModel(androidApplication(), getScope(Constants.SCOPE_ID_NOTE).get(), pageIndex) }
//
//    viewModel { CameraViewModel(androidApplication(), getScope(Constants.SCOPE_ID_NOTE).get()) }
//
//    viewModel { PageManageViewModel(androidApplication(), getScope(Constants.SCOPE_ID_NOTE).get()) }

    viewModel { NoteViewModel(androidApplication(), getScope(Constants.SCOPE_ID_NOTE).get(), get()) }

    viewModel { NoteListViewModel(androidApplication(), get()) }

//    viewModel { SlideShowViewModel(androidApplication(), getScope(Constants.SCOPE_ID_NOTE).get()) }
}