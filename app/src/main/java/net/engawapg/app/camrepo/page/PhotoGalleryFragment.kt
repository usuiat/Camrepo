package net.engawapg.app.camrepo.page

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.engawapg.app.camrepo.databinding.FragmentPhotoGalleryBinding
import org.koin.android.viewmodel.ViewModelOwner
import org.koin.android.viewmodel.ext.android.sharedViewModel

class PhotoGalleryFragment : Fragment() {

    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoGalleryViewModel by sharedViewModel(owner = {
        ViewModelOwner.from(
            requireParentFragment()
        )
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        Log.d("PhotoGalleryFragment", "onDestroyView")
    }
}