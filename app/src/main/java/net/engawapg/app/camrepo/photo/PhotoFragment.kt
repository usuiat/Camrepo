package net.engawapg.app.camrepo.photo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.fragment_photo.*
import kotlinx.coroutines.launch
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.sharedViewModel

private const val ARG_PHOTO_INDEX = "photoIndex"

/**
 * A simple [Fragment] subclass.
 * Use the [PhotoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PhotoFragment : Fragment() {
    private val viewModel: PhotoViewModel by sharedViewModel()
    private var photoIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoIndex = it.getInt(ARG_PHOTO_INDEX, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "viewModel = $viewModel")
        Log.d(TAG, "photoIndex = $photoIndex")

        val imageInfo = viewModel.getPhotoAt(photoIndex)
        viewLifecycleOwner.lifecycleScope.launch {
            val resolver = context?.contentResolver
            val bmp = resolver?.let { imageInfo?.getBitmapWithResolver(it) }
            photoView.setImageBitmap(bmp)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(photoIndex: Int) =
            PhotoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PHOTO_INDEX, photoIndex)
                }
            }

        private const val TAG = "PhotoFragment"
    }
}