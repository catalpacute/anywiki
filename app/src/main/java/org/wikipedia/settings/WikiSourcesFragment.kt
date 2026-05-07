package org.wikipedia.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.anywiki.WikiSourceRepository
import org.wikipedia.anywiki.mediawiki.DiscoverFailure
import org.wikipedia.anywiki.mediawiki.DiscoveryResult
import org.wikipedia.anywiki.mediawiki.MediaWikiClient
import org.wikipedia.anywiki.mediawiki.MediaWikiDiscover
import org.wikipedia.anywiki.mediawiki.WikiSiteConfig
import org.wikipedia.util.FeedbackUtil
import work.czzzz.anywiki.R
import work.czzzz.anywiki.databinding.FragmentWikiSourcesBinding
import work.czzzz.anywiki.databinding.ItemWikiSourceBinding

class WikiSourcesFragment : Fragment() {
    private var _binding: FragmentWikiSourcesBinding? = null
    private val binding get() = _binding!!
    private val adapter = WikiSourceAdapter(
        onSetActive = { source ->
            WikiSourceRepository.setActiveSource(source.id)
            refresh()
            FeedbackUtil.showMessage(requireActivity(), R.string.anywiki_message_active_source_changed)
        },
        onRemove = { source ->
            WikiSourceRepository.removeSource(source.id)
            refresh()
            FeedbackUtil.showMessage(requireActivity(), R.string.anywiki_message_wiki_removed)
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWikiSourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.wikiSourcesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.wikiSourcesRecyclerView.adapter = adapter
        binding.wikiSourceAddButton.setOnClickListener { addSource() }
        refresh()
    }

    override fun onDestroyView() {
        binding.wikiSourcesRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun refresh() {
        val activeId = WikiSourceRepository.getActiveSource().id
        val sources = WikiSourceRepository.getSources()
        adapter.submitList(sources, activeId)
        binding.wikiSourcesEmpty.visibility = if (sources.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun addSource() {
        val input = binding.wikiSourceInputEdit.text?.toString().orEmpty()
        binding.wikiSourceAddButton.isEnabled = false
        binding.wikiSourceProgress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            if (!isAdded) {
                return@CoroutineExceptionHandler
            }
            binding.wikiSourceAddButton.isEnabled = true
            binding.wikiSourceProgress.visibility = View.GONE
            FeedbackUtil.showError(requireActivity(), throwable)
        }) {
            when (val result = MediaWikiDiscover.discover(input, MediaWikiClient())) {
                is DiscoveryResult.Success -> {
                    binding.wikiSourceAddButton.isEnabled = true
                    binding.wikiSourceProgress.visibility = View.GONE
                    if (WikiSourceRepository.addSource(result.config)) {
                        binding.wikiSourceInputEdit.setText("")
                        FeedbackUtil.showMessage(requireActivity(), getString(R.string.anywiki_message_wiki_added, result.config.displayName))
                    } else {
                        FeedbackUtil.showMessage(requireActivity(), R.string.anywiki_message_wiki_exists)
                    }
                    refresh()
                }
                is DiscoveryResult.Failure -> {
                    binding.wikiSourceAddButton.isEnabled = true
                    binding.wikiSourceProgress.visibility = View.GONE
                    FeedbackUtil.showMessage(requireActivity(), failureMessage(result.reason))
                }
            }
        }
    }

    private fun failureMessage(failure: DiscoverFailure): Int {
        return when (failure) {
            DiscoverFailure.INVALID_URL -> R.string.anywiki_error_invalid_url
            DiscoverFailure.NOT_MEDIAWIKI -> R.string.anywiki_error_not_mediawiki
            DiscoverFailure.API_UNREACHABLE -> R.string.anywiki_error_api_unreachable
            DiscoverFailure.API_DISABLED -> R.string.anywiki_error_api_disabled
            DiscoverFailure.NETWORK_OFFLINE -> R.string.anywiki_error_network_offline
        }
    }

    companion object {
        fun newInstance(): WikiSourcesFragment {
            return WikiSourcesFragment()
        }
    }
}

private class WikiSourceAdapter(
    private val onSetActive: (WikiSiteConfig) -> Unit,
    private val onRemove: (WikiSiteConfig) -> Unit
) : RecyclerView.Adapter<WikiSourceViewHolder>() {
    private val items = mutableListOf<WikiSiteConfig>()
    private var activeId: String? = null

    fun submitList(newItems: List<WikiSiteConfig>, activeSourceId: String) {
        items.clear()
        items.addAll(newItems)
        activeId = activeSourceId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WikiSourceViewHolder {
        val binding = ItemWikiSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WikiSourceViewHolder(binding, onSetActive, onRemove)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: WikiSourceViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == activeId)
    }
}

private class WikiSourceViewHolder(
    private val binding: ItemWikiSourceBinding,
    private val onSetActive: (WikiSiteConfig) -> Unit,
    private val onRemove: (WikiSiteConfig) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: WikiSiteConfig, isActive: Boolean) {
        binding.wikiSourceName.text = item.displayName
        binding.wikiSourceBaseUrl.text = item.baseUrl
        binding.wikiSourceApiUrl.text = item.apiUrl
        binding.wikiSourceStatus.visibility = if (isActive) View.VISIBLE else View.GONE
        binding.wikiSourceSetActiveButton.isEnabled = !isActive
        binding.wikiSourceSetActiveButton.setOnClickListener { onSetActive(item) }
        binding.wikiSourceRemoveButton.setOnClickListener { onRemove(item) }
    }
}
