package org.wikipedia.main

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
import org.wikipedia.WikipediaApp
import org.wikipedia.anywiki.WikiSourceRepository
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.WikiSourcesActivity
import work.czzzz.anywiki.R
import work.czzzz.anywiki.databinding.FragmentSourceHomeBinding
import work.czzzz.anywiki.databinding.ItemSourceHomeHistoryBinding
import java.text.DateFormat

class SourceHomeFragment : Fragment() {
    private var _binding: FragmentSourceHomeBinding? = null
    private val binding get() = _binding!!
    private val adapter = RecentHistoryAdapter(::openHistoryEntry)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSourceHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sourceRecentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.sourceRecentRecyclerView.adapter = adapter
        binding.sourceOpenMainPageButton.setOnClickListener { openMainPage() }
        binding.sourceManageButton.setOnClickListener {
            startActivity(WikiSourcesActivity.newIntent(requireContext()))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroyView() {
        binding.sourceRecentRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }

    fun refresh() {
        val source = WikiSourceRepository.getActiveSource()
        binding.sourceNameText.text = source.displayName
        binding.sourceUrlText.text = source.baseUrl
        binding.sourceApiText.text = source.apiUrl

        viewLifecycleOwner.lifecycleScope.launch(CoroutineExceptionHandler { _, _ ->
            if (!isAdded) {
                return@CoroutineExceptionHandler
            }
            adapter.submitList(emptyList())
            binding.sourceRecentEmpty.visibility = View.VISIBLE
        }) {
            val activeSite = WikipediaApp.instance.wikiSite
            val entries = AppDatabase.instance.historyEntryWithImageDao()
                .getHistoryEntriesWithOffset(limit = 24, offset = 0)
                .filter { it.matchesSource(activeSite) }
                .take(12)
            adapter.submitList(entries)
            binding.sourceRecentEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    fun scrollToTop() {
        binding.sourceRecentRecyclerView.scrollToPosition(0)
    }

    private fun openMainPage() {
        val wikiSite = WikipediaApp.instance.wikiSite
        val title = PageTitle(wikiSite.mainPageTitle(), wikiSite)
        val entry = HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE)
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, title))
    }

    private fun openHistoryEntry(item: HistoryEntryWithImage) {
        val authority = if (item.authority.startsWith("http://") || item.authority.startsWith("https://")) {
            item.authority
        } else {
            "${WikipediaApp.instance.wikiSite.scheme()}://${item.authority}"
        }
        val title = PageTitle(item.namespace, item.apiTitle, WikiSite(authority, item.lang))
        title.displayText = item.displayTitle
        title.description = item.description
        val entry = HistoryEntry(
            authority = authority,
            lang = item.lang,
            apiTitle = item.apiTitle,
            displayTitle = item.displayTitle,
            id = item.id,
            namespace = item.namespace,
            timestamp = item.timestamp,
            source = HistoryEntry.SOURCE_HISTORY
        )
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, title))
    }

    companion object {
        fun newInstance(): SourceHomeFragment {
            return SourceHomeFragment()
        }
    }
}

private fun HistoryEntryWithImage.matchesSource(site: WikiSite): Boolean {
    return authority == site.url() || authority == site.authority()
}

private class RecentHistoryAdapter(
    private val onClick: (HistoryEntryWithImage) -> Unit
) : RecyclerView.Adapter<RecentHistoryViewHolder>() {
    private val items = mutableListOf<HistoryEntryWithImage>()

    fun submitList(newItems: List<HistoryEntryWithImage>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentHistoryViewHolder {
        val binding = ItemSourceHomeHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentHistoryViewHolder(binding, onClick)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecentHistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }
}

private class RecentHistoryViewHolder(
    private val binding: ItemSourceHomeHistoryBinding,
    private val onClick: (HistoryEntryWithImage) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    fun bind(item: HistoryEntryWithImage) {
        binding.sourceHistoryTitle.text = item.displayTitle
        binding.sourceHistoryMeta.text = dateFormat.format(item.timestamp)
        binding.sourceHistoryDescription.text = item.description.orEmpty()
        binding.root.setOnClickListener { onClick(item) }
    }
}
