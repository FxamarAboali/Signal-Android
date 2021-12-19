package org.thoughtcrime.securesms.keyboard.gif

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import org.thoughtcrime.securesms.LoggingFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.giph.mp4.GiphyMp4Fragment
import org.thoughtcrime.securesms.giph.mp4.GiphyMp4SaveResult
import org.thoughtcrime.securesms.giph.mp4.GiphyMp4ViewModel
import org.thoughtcrime.securesms.keyboard.emoji.KeyboardPageSearchView
import org.thoughtcrime.securesms.keyboard.findListener
import org.thoughtcrime.securesms.util.ViewUtil
import org.thoughtcrime.securesms.util.views.SimpleProgressDialog

class GifKeyboardPageFragment : LoggingFragment(R.layout.gif_keyboard_page_fragment) {

  private lateinit var host: Host
  private lateinit var quickSearchAdapter: GifQuickSearchAdapter
  private lateinit var giphyMp4ViewModel: GiphyMp4ViewModel

  private lateinit var viewModel: GifKeyboardPageViewModel

  private var progressDialog: AlertDialog? = null
  private lateinit var quickSearchList: RecyclerView

  private lateinit var searchKeyboard: KeyboardPageSearchView
  private lateinit var presetsBar: LinearLayout

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    host = findListener<Host>() ?: throw AssertionError("Parent fragment or activity must implement Host")

    childFragmentManager.beginTransaction()
      .replace(R.id.gif_keyboard_giphy_frame, GiphyMp4Fragment.create(host.isMms()))
      .commitAllowingStateLoss()

    presetsBar = view.findViewById<LinearLayout>(R.id.gif_quick_search)

    searchKeyboard = view.findViewById(R.id.gif_keyboard_search_text)
    searchKeyboard.enableBackNavigation()
    searchKeyboard.callbacks = object : KeyboardPageSearchView.Callbacks {
      override fun onClicked() {
        ViewUtil.focusAndShowKeyboard(searchKeyboard)
      }

      override fun onQueryChanged(query: String) {
        giphyMp4ViewModel.updateSearchQuery(query)
      }

      override fun onFocusLost() {
        ViewUtil.hideKeyboard(requireContext(), requireView())
      }

      override fun onNavigationClicked() {
        ViewUtil.hideKeyboard(requireContext(), requireView())
        switchBetweenSearchAndPresets()
        searchKeyboard.clearQuery()
      }
    }

    view.findViewById<View>(R.id.gif_keyboard_search).setOnClickListener {
      switchBetweenSearchAndPresets()
      if(searchKeyboard.visibility === View.VISIBLE) ViewUtil.focusAndShowKeyboard(searchKeyboard)
      if(presetsBar.visibility === View.VISIBLE) giphyMp4ViewModel.updateSearchQuery(viewModel.selectedTab.query)
    }

    quickSearchList = view.findViewById(R.id.gif_keyboard_quick_search_recycler)
    quickSearchAdapter = GifQuickSearchAdapter(this::onQuickSearchSelected)
    quickSearchList.adapter = quickSearchAdapter

    giphyMp4ViewModel = ViewModelProvider(requireActivity(), GiphyMp4ViewModel.Factory(host.isMms())).get(GiphyMp4ViewModel::class.java)
    giphyMp4ViewModel.saveResultEvents.observe(viewLifecycleOwner, this::handleGiphyMp4SaveResult)
  }

  @Suppress("DEPRECATION")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel = ViewModelProvider(requireActivity()).get(GifKeyboardPageViewModel::class.java)
    updateQuickSearchTabs()
  }

  private fun switchBetweenSearchAndPresets() {
    fun toggleVisibility(view: View) {
      if(view.visibility === View.VISIBLE) {
        view.visibility = View.INVISIBLE
      } else {
        view.visibility = View.VISIBLE
      }
    }
    // searchKeyboard is invisible by default so we will never
    // run into the case where both views are shown together
    toggleVisibility(searchKeyboard)
    toggleVisibility(presetsBar)
  }

  private fun onQuickSearchSelected(gifQuickSearchOption: GifQuickSearchOption) {
    if (viewModel.selectedTab == gifQuickSearchOption) {
      return
    }

    viewModel.selectedTab = gifQuickSearchOption
    giphyMp4ViewModel.updateSearchQuery(gifQuickSearchOption.query)

    updateQuickSearchTabs()
  }

  private fun updateQuickSearchTabs() {
    val quickSearches: List<GifQuickSearch> = GifQuickSearchOption.ranked
      .map { search -> GifQuickSearch(search, search == viewModel.selectedTab) }

    quickSearchAdapter.submitList(quickSearches, this::scrollToTab)
  }

  private fun scrollToTab() {
    quickSearchList.post { quickSearchList.smoothScrollToPosition(GifQuickSearchOption.ranked.indexOf(viewModel.selectedTab)) }
  }

  private fun handleGiphyMp4SaveResult(result: GiphyMp4SaveResult) {
    if (result is GiphyMp4SaveResult.Success) {
      hideProgressDialog()
      handleGiphyMp4SuccessfulResult(result)
    } else if (result is GiphyMp4SaveResult.Error) {
      hideProgressDialog()
      handleGiphyMp4ErrorResult()
    } else {
      progressDialog = SimpleProgressDialog.show(requireContext())
    }
  }

  private fun hideProgressDialog() {
    progressDialog?.dismiss()
  }

  private fun handleGiphyMp4SuccessfulResult(success: GiphyMp4SaveResult.Success) {
    host.onGifSelectSuccess(success.blobUri, success.width, success.height)
  }

  private fun handleGiphyMp4ErrorResult() {
    Toast.makeText(requireContext(), R.string.GiphyActivity_error_while_retrieving_full_resolution_gif, Toast.LENGTH_LONG).show()
  }

  interface Host {
    fun isMms(): Boolean
    fun onGifSelectSuccess(blobUri: Uri, width: Int, height: Int)
  }
}
