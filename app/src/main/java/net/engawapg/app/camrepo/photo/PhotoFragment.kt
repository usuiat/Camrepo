package net.engawapg.app.camrepo.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentPhotoBinding
import org.koin.android.viewmodel.ViewModelOwner.Companion.from
import org.koin.android.viewmodel.ext.android.sharedViewModel

private const val ARG_PHOTO_INDEX = "photoIndex"

/**
 * A simple [Fragment] subclass.
 * Use the [PhotoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PhotoFragment : Fragment() {
    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoViewModel by sharedViewModel(owner = {from(requireParentFragment())})
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
    ): View {
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageInfo = viewModel.getPhotoAt(photoIndex)
        Picasso.get()
            .load(imageInfo?.uri)
            .error(R.drawable.imagenotfound)
            .fit()
            .centerInside()
            .into(binding.photoView)
    }

    companion object {
        @JvmStatic
        fun newInstance(photoIndex: Int) =
            PhotoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PHOTO_INDEX, photoIndex)
                }
            }
    }
}