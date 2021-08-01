package net.engawapg.app.camrepo.page

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.PermissionChecker
import androidx.fragment.app.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentPhotoGalleryBinding
import net.engawapg.app.camrepo.databinding.ViewPhotoGalleryImageBinding
import net.engawapg.app.camrepo.util.SimpleDialog
import org.koin.android.viewmodel.ViewModelOwner
import org.koin.android.viewmodel.ext.android.sharedViewModel

class PhotoGalleryFragment : Fragment(), SimpleDialog.ResultListener {

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
                SimpleDialog.Builder()
                    .setMessage(R.string.permission_req_for_gallery)
                    .setPositiveText("OK")
                    .setNegativeText("Cancel")
                    .create()
                    .show(childFragmentManager, "RationalDialog")
            } else {
                /* 拒否（ファイナルアンサー）*/
                viewModel.isPermissionDenied.value = true
            }
        }
    }

    override fun onSimpleDialogResult(tag: String?, result: SimpleDialog.Result) {
        if (result == SimpleDialog.Result.POSITIVE) {
            /* Permissionを要求 */
            permissionRequest.launch(REQ_PERMISSION)
        } else {
            /* あきらめる */
            viewModel.isPermissionDenied.value = true
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
        binding.lifecycleOwner = viewLifecycleOwner

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