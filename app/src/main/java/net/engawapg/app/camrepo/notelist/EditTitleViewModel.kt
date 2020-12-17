package net.engawapg.app.camrepo.notelist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class EditTitleViewModel(app: Application): AndroidViewModel(app) {
    var dialogTitle: String = ""
    var title: String = ""
    var subTitle: String = ""
    val onClickOk = MutableLiveData<Boolean>()
}