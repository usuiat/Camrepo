package net.engawapg.app.camrepo.page

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.PermissionChecker
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentPhotoGalleryBinding
import net.engawapg.app.camrepo.databinding.ViewPhotoGalleryImageBinding
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

    /* Permissionの結果を受け取る */
    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            /* 承認された。*/
            viewModel.isPermissionGranted.value = true
        } else {
            /* 拒否された */
            if (shouldShowRequestPermissionRationale(REQ_PERMISSION)) {
                /* Permissionの必要性を説明するダイアログを表示する */
                showRationaleDialog()
            } else {
                /* 拒否（ファイナルアンサー）*/
                viewModel.isPermissionDenied.value = true
            }
        }
    }

    /* Permissionの必要性を説明するダイアログを表示する */
    private fun showRationaleDialog() {
        RationaleDialog().show(childFragmentManager, viewLifecycleOwner) {
            if (it == RationaleDialog.RESULT_OK) {
                /* Permissionを要求 */
                permissionRequest.launch(REQ_PERMISSION)
            } else {
                /* あきらめる */
                viewModel.isPermissionDenied.value = true
            }
        }
    }

    class RationaleDialog: DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return activity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.permission_req_for_gallery)
                    .setPositiveButton("OK") { _, _ ->
                        setFragmentResult(REQUEST_KEY, bundleOf(Pair(RESULT_KEY, RESULT_OK)))
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        setFragmentResult(REQUEST_KEY, bundleOf(Pair(RESULT_KEY, RESULT_CANCEL)))
                    }
                    .create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }

        fun show(manager: FragmentManager, lifecycleOwner: LifecycleOwner, callback: (Int)->Unit) {
            val listener = FragmentResultListener { _, result ->
                val resultValue = result.getInt(RESULT_KEY)
                callback(resultValue)
            }
            manager.setFragmentResultListener(REQUEST_KEY, lifecycleOwner, listener)
            show(manager, TAG)
        }

        companion object {
            private const val TAG = "TAG_RATIONALE_DIALOG"
            private const val REQUEST_KEY = "RATIONALE_DIALOG_REQUEST_KEY"
            private const val RESULT_KEY = "RATIONALE_DIALOG_RESULT_KEY"
            const val RESULT_OK = 1
            const val RESULT_CANCEL = -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context?.let {
            /* 必要なPermissionが与えられているかどうかを確認 */
            val result = PermissionChecker.checkSelfPermission(it, REQ_PERMISSION)
            if (result == PermissionChecker.PERMISSION_GRANTED) {
                /* 承認済み */
                viewModel.isPermissionGranted.value = true
            } else {
                /* 要求する */
                permissionRequest.launch(REQ_PERMISSION)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner //TODO 必要？

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* RecyclerViewの設定 */
        val imageAdapter = ImageAdapter()
        /* リストは後から更新する */
        viewModel.photoList.observe(viewLifecycleOwner, {
            imageAdapter.submitList(it)
        })
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@PhotoGalleryFragment.context, SPAN_COUNT)
            adapter = imageAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        /* Permission承認済みなら、デバイスの写真にアクセスする */
        if (viewModel.isPermissionGranted.value == true){
            viewModel.loadPhotoList()
        }
    }

    companion object {
        private const val SPAN_COUNT = 4
        private const val REQ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    }

    inner class ImageAdapter
        : ListAdapter<PhotoGalleryItem, ImageViewHolder>(PhotoGalleryItem.DIFF_UTIL) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ViewPhotoGalleryImageBinding.inflate(inflater, parent, false)
            return ImageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val item = viewModel.getPhotoItem(position)

            holder.binding.viewModel = viewModel
            holder.binding.item = item
            holder.binding.executePendingBindings()

            item?.let {
                /* Picassoライブラリを使って非同期に画像を読み込む */
                Picasso.get().load(it.uri)
                    .fit()
                    .centerCrop()
                    .into(holder.binding.imageView)
            }
        }
    }

    inner class ImageViewHolder(val binding: ViewPhotoGalleryImageBinding)
        : RecyclerView.ViewHolder(binding.root)
}