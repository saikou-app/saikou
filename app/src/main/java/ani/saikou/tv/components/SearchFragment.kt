package ani.saikou.tv.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.CompletionInfo
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.leanback.R
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.*

/**
 * A fragment to handle searches. An application will supply an implementation
 * of the [SearchResultProvider] interface to handle the search and return
 * an [ObjectAdapter] containing the results. The results are rendered
 * into a [RowsSupportFragment], in the same way that they are in a [ ].
 *
 *
 * A SpeechRecognizer object will be created for which your application will need to declare
 * android.permission.RECORD_AUDIO in AndroidManifest file. If app's target version is >= 23 and
 * the device version is >= 23, a permission dialog will show first time using speech recognition.
 * 0 will be used as requestCode in requestPermissions() call.
 * [.setSpeechRecognitionCallback] is deprecated.
 *
 *
 *
 * Speech recognition is automatically started when fragment is created, but
 * not when fragment is restored from an instance state.  Activity may manually
 * call [.startRecognition], typically in onNewIntent().
 *
 */

open class SearchFragment : Fragment() {

    val mAdapterObserver: ObjectAdapter.DataObserver = object : ObjectAdapter.DataObserver() {
        override fun onChanged() {
            // onChanged() may be called multiple times e.g. the provider add
            // rows to ArrayObjectAdapter one by one.
            mHandler.removeCallbacks(mResultsChangedCallback)
            mHandler.post(mResultsChangedCallback)
        }
    }
    val mHandler = Handler()
    val mResultsChangedCallback: Runnable = object : Runnable {
        override fun run() {
            if (DEBUG) Log.v(
                TAG,
                "results changed, new size " + mResultAdapter!!.size()
            )
            if (rowsSupportFragment != null
                && rowsSupportFragment!!.adapter !== mResultAdapter
            ) {
                if (!(rowsSupportFragment!!.adapter == null && mResultAdapter!!.size() == 0)) {
                    rowsSupportFragment!!.adapter = mResultAdapter
                    rowsSupportFragment!!.setSelectedPosition(0)
                }
            }
            updateSearchBarVisibility()
            mStatus = mStatus or RESULTS_CHANGED
            if (mStatus and QUERY_COMPLETE != 0) {
                updateFocus()
            }
            updateSearchBarNextFocusId()
        }
    }

    /**
     * Runs when a new provider is set AND when the fragment view is created.
     */
    private val mSetSearchResultProvider: Runnable = object : Runnable {
        override fun run() {
            if (rowsSupportFragment == null) {
                // We'll retry once we have a rows fragment
                return
            }
            // Retrieve the result adapter
            val adapter = mProvider!!.resultsAdapter
            if (DEBUG) Log.v(
                TAG,
                "Got results adapter $adapter"
            )
            if (adapter !== mResultAdapter) {
                val firstTime = mResultAdapter == null
                releaseAdapter()
                mResultAdapter = adapter
                if (mResultAdapter != null) {
                    mResultAdapter!!.registerObserver(mAdapterObserver)
                }
                if (DEBUG) {
                    Log.v(
                        TAG,
                        "mResultAdapter " + mResultAdapter + " size "
                                + if (mResultAdapter == null) 0 else mResultAdapter!!.size()
                    )
                }
                // delay the first time to avoid setting a empty result adapter
                // until we got first onChange() from the provider
                if (!(firstTime && (mResultAdapter == null || mResultAdapter!!.size() == 0))) {
                    rowsSupportFragment!!.adapter = mResultAdapter
                }
                executePendingQuery()
            }
            updateSearchBarNextFocusId()
            if (DEBUG) {
                Log.v(
                    TAG,
                    "mAutoStartRecognition " + mAutoStartRecognition
                            + " mResultAdapter " + mResultAdapter
                            + " adapter " + rowsSupportFragment!!.adapter
                )
            }
            if (mAutoStartRecognition) {
                mHandler.removeCallbacks(mStartRecognitionRunnable)
                mHandler.postDelayed(
                    mStartRecognitionRunnable,
                    SPEECH_RECOGNITION_DELAY_MS
                )
            } else {
                updateFocus()
            }
        }
    }
    val mStartRecognitionRunnable: Runnable = Runnable {
        mAutoStartRecognition = false
        mSearchBar!!.startRecognition()
    }

    /**
     * Returns RowsSupportFragment that shows result rows. RowsSupportFragment is initialized after
     * SearchSupportFragment.onCreateView().
     *
     * @return RowsSupportFragment that shows result rows.
     */
    var rowsSupportFragment: VerticalGridSupportFragment? = null
    var mSearchBar: SearchBar? = null
    var mProvider: SearchSupportFragment.SearchResultProvider? = null
    var mPendingQuery: String? = null
    var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null
    private var mOnItemViewClickedListener: OnItemViewClickedListener? = null
    var mResultAdapter: ObjectAdapter? = null
    private var mSpeechRecognitionCallback: SpeechRecognitionCallback? = null
    private var mTitle: String? = null
    private var mBadgeDrawable: Drawable? = null
    private var mExternalQuery: ExternalQuery? = null
    private var mSpeechRecognizer: SpeechRecognizer? = null
    var mStatus = 0
    var mAutoStartRecognition = true
    private var mIsPaused = false
    private var mPendingStartRecognitionWhenPaused = false
    private val mPermissionListener: SearchBar.SearchBarPermissionListener = SearchBar.SearchBarPermissionListener {
        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE && permissions.size > 0) {
            if (permissions[0] == Manifest.permission.RECORD_AUDIO && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecognition()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (mAutoStartRecognition) {
            mAutoStartRecognition = savedInstanceState == null
        }
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(ani.saikou.R.layout.tv_search_fragment, container, false)
        val searchFrame = root.findViewById<View>(R.id.lb_search_frame) as FrameLayout
        mSearchBar = searchFrame.findViewById<View>(R.id.lb_search_bar) as SearchBar
        mSearchBar!!.setSearchBarListener(object : SearchBar.SearchBarListener {
            override fun onSearchQueryChange(query: String) {
                if (DEBUG) Log.v(
                    TAG, String.format(
                        "onSearchQueryChange %s %s", query,
                        if (null == mProvider) "(null)" else mProvider
                    )
                )
                if (null != mProvider) {
                    retrieveResults(query)
                } else {
                    mPendingQuery = query
                }
            }

            override fun onSearchQuerySubmit(query: String) {
                if (DEBUG) Log.v(
                    TAG,
                    String.format("onSearchQuerySubmit %s", query)
                )
                submitQuery(query)
            }

            override fun onKeyboardDismiss(query: String) {
                if (DEBUG) Log.v(
                    TAG,
                    String.format("onKeyboardDismiss %s", query)
                )
                queryComplete()
            }
        })
        mSearchBar!!.setSpeechRecognitionCallback(mSpeechRecognitionCallback)
        mSearchBar!!.setPermissionListener(mPermissionListener)
        applyExternalQuery()
        readArguments(arguments)
        if (null != mBadgeDrawable) {
            badgeDrawable = mBadgeDrawable
        }
        if (null != mTitle) {
            title = mTitle
        }

        // Inject the RowsSupportFragment in the results container
        if (childFragmentManager.findFragmentById(R.id.lb_results_frame) == null) {
            rowsSupportFragment = VerticalGridSupportFragment()
            val presenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE,false)
            presenter.shadowEnabled = false
            presenter.numberOfColumns = 5
            rowsSupportFragment!!.gridPresenter = presenter
            childFragmentManager.beginTransaction()
                .replace(R.id.lb_results_frame, rowsSupportFragment!!).commit()
        } else {
            rowsSupportFragment = childFragmentManager
                .findFragmentById(R.id.lb_results_frame) as VerticalGridSupportFragment?
        }

        rowsSupportFragment!!.setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            updateSearchBarVisibility()
            if (null != mOnItemViewSelectedListener) {
                mOnItemViewSelectedListener!!.onItemSelected(
                    itemViewHolder, item,
                    rowViewHolder, row
                )
            }
        }

        rowsSupportFragment!!.onItemViewClickedListener = mOnItemViewClickedListener
        //rowsSupportFragment!!.setExpand(true)
        if (null != mProvider) {
            onSetSearchResultProvider()
        }
        return root
    }

    private fun resultsAvailable() {
        if (mStatus and QUERY_COMPLETE != 0) {
            focusOnResults()
        }
        updateSearchBarNextFocusId()
    }

    override fun onStart() {
        super.onStart()
        /*val list = rowsSupportFragment!!.verticalGridView
        val mContainerListAlignTop =
            resources.getDimensionPixelSize(R.dimen.lb_search_browse_rows_align_top)
        list.itemAlignmentOffset = 0
        list.itemAlignmentOffsetPercent = VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
        list.windowAlignmentOffset = mContainerListAlignTop
        list.windowAlignmentOffsetPercent = VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        list.windowAlignment = VerticalGridView.WINDOW_ALIGN_NO_EDGE
        // VerticalGridView should not be focusable (see b/26894680 for details).
        list.isFocusable = false
        list.isFocusableInTouchMode = false*/
    }

    override fun onResume() {
        super.onResume()
        mIsPaused = false
        if (mSpeechRecognitionCallback == null && null == mSpeechRecognizer) {
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(
                context
            )
            mSearchBar!!.setSpeechRecognizer(mSpeechRecognizer)
        }
        if (mPendingStartRecognitionWhenPaused) {
            mPendingStartRecognitionWhenPaused = false
            mSearchBar!!.startRecognition()
        } else {
            // Ensure search bar state consistency when using external recognizer
            mSearchBar!!.stopRecognition()
        }
    }

    override fun onPause() {
        releaseRecognizer()
        mIsPaused = true
        super.onPause()
    }

    override fun onDestroy() {
        releaseAdapter()
        super.onDestroy()
    }

    private fun releaseRecognizer() {
        if (null != mSpeechRecognizer) {
            mSearchBar!!.setSpeechRecognizer(null)
            mSpeechRecognizer!!.destroy()
            mSpeechRecognizer = null
        }
    }

    /**
     * Starts speech recognition.  Typical use case is that
     * activity receives onNewIntent() call when user clicks a MIC button.
     * Note that SearchSupportFragment automatically starts speech recognition
     * at first time created, there is no need to call startRecognition()
     * when fragment is created.
     */
    fun startRecognition() {
        if (mIsPaused) {
            mPendingStartRecognitionWhenPaused = true
        } else {
            mSearchBar!!.startRecognition()
        }
    }

    /**
     * Sets the search provider that is responsible for returning results for the
     * search query.
     */
    fun setSearchResultProvider(searchResultProvider: SearchSupportFragment.SearchResultProvider) {
        if (mProvider !== searchResultProvider) {
            mProvider = searchResultProvider
            onSetSearchResultProvider()
        }
    }

    /**
     * Sets an item selection listener for the results.
     *
     * @param listener The item selection listener to be invoked when an item in
     * the search results is selected.
     */
    fun setOnItemViewSelectedListener(listener: OnItemViewSelectedListener?) {
        mOnItemViewSelectedListener = listener
    }

    /**
     * Sets an item clicked listener for the results.
     *
     * @param listener The item clicked listener to be invoked when an item in
     * the search results is clicked.
     */
    fun setOnItemViewClickedListener(listener: OnItemViewClickedListener) {
        if (listener !== mOnItemViewClickedListener) {
            mOnItemViewClickedListener = listener
            if (rowsSupportFragment != null) {
                rowsSupportFragment!!.onItemViewClickedListener = mOnItemViewClickedListener
            }
        }
    }
    /**
     * Returns the title set in the search bar.
     */
    /**
     * Sets the title string to be be shown in an empty search bar. The title
     * may be placed in a call-to-action, such as "Search *title*" or
     * "Speak to search *title*".
     */
    var title: String?
        get() = if (null != mSearchBar) {
            mSearchBar!!.title
        } else null
        set(title) {
            mTitle = title
            if (null != mSearchBar) {
                mSearchBar!!.title = title
            }
        }
    /**
     * Returns the badge drawable in the search bar.
     */
    /**
     * Sets the badge drawable that will be shown inside the search bar next to
     * the title.
     */
    var badgeDrawable: Drawable?
        get() {
            return if (null != mSearchBar) {
                mSearchBar!!.badgeDrawable
            } else null
        }
        set(drawable) {
            mBadgeDrawable = drawable
            if (null != mSearchBar) {
                mSearchBar!!.badgeDrawable = drawable
            }
        }

    /**
     * Sets background color of not-listening state search orb.
     *
     * @param colors SearchOrbView.Colors.
     */
    fun setSearchAffordanceColors(colors: SearchOrbView.Colors?) {
        if (mSearchBar != null) {
            mSearchBar!!.setSearchAffordanceColors(colors)
        }
    }

    /**
     * Sets background color of listening state search orb.
     *
     * @param colors SearchOrbView.Colors.
     */
    fun setSearchAffordanceColorsInListening(colors: SearchOrbView.Colors?) {
        if (mSearchBar != null) {
            mSearchBar!!.setSearchAffordanceColorsInListening(colors)
        }
    }

    /**
     * Displays the completions shown by the IME. An application may provide
     * a list of query completions that the system will show in the IME.
     *
     * @param completions A list of completions to show in the IME. Setting to
     * null or empty will clear the list.
     */
    fun displayCompletions(completions: List<String?>?) {
        mSearchBar!!.displayCompletions(completions)
    }

    /**
     * Displays the completions shown by the IME. An application may provide
     * a list of query completions that the system will show in the IME.
     *
     * @param completions A list of completions to show in the IME. Setting to
     * null or empty will clear the list.
     */
    fun displayCompletions(completions: Array<CompletionInfo?>?) {
        mSearchBar!!.displayCompletions(completions)
    }

    /**
     * Sets this callback to have the fragment pass speech recognition requests
     * to the activity rather than using a SpeechRecognizer object.
     */
    @Deprecated(
        """Launching voice recognition activity is no longer supported. App should declare
                  android.permission.RECORD_AUDIO in AndroidManifest file."""
    )
    fun setSpeechRecognitionCallback(callback: SpeechRecognitionCallback?) {
        mSpeechRecognitionCallback = callback
        if (mSearchBar != null) {
            mSearchBar!!.setSpeechRecognitionCallback(mSpeechRecognitionCallback)
        }
        if (callback != null) {
            releaseRecognizer()
        }
    }

    /**
     * Sets the text of the search query and optionally submits the query. Either
     * [onQueryTextChange][SearchResultProvider.onQueryTextChange] or
     * [onQueryTextSubmit][SearchResultProvider.onQueryTextSubmit] will be
     * called on the provider if it is set.
     *
     * @param query The search query to set.
     * @param submit Whether to submit the query.
     */
    fun setSearchQuery(query: String?, submit: Boolean) {
        if (DEBUG) Log.v(
            TAG,
            "setSearchQuery $query submit $submit"
        )
        if (query == null) {
            return
        }
        mExternalQuery = ExternalQuery(query, submit)
        applyExternalQuery()
        if (mAutoStartRecognition) {
            mAutoStartRecognition = false
            mHandler.removeCallbacks(mStartRecognitionRunnable)
        }
    }

    /**
     * Sets the text of the search query based on the [RecognizerIntent.EXTRA_RESULTS] in
     * the given intent, and optionally submit the query.  If more than one result is present
     * in the results list, the first will be used.
     *
     * @param intent Intent received from a speech recognition service.
     * @param submit Whether to submit the query.
     */
    fun setSearchQuery(intent: Intent, submit: Boolean) {
        val matches = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (matches != null && matches.size > 0) {
            setSearchQuery(matches[0], submit)
        }
    }

    /**
     * Returns an intent that can be used to request speech recognition.
     * Built from the base [RecognizerIntent.ACTION_RECOGNIZE_SPEECH] plus
     * extras:
     *
     *
     *  * [RecognizerIntent.EXTRA_LANGUAGE_MODEL] set to
     * [RecognizerIntent.LANGUAGE_MODEL_FREE_FORM]
     *  * [RecognizerIntent.EXTRA_PARTIAL_RESULTS] set to true
     *  * [RecognizerIntent.EXTRA_PROMPT] set to the search bar hint text
     *
     *
     * For handling the intent returned from the service, see
     * [.setSearchQuery].
     */
    val recognizerIntent: Intent
        get() {
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            if (mSearchBar != null && mSearchBar!!.hint != null) {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, mSearchBar!!.hint)
            }
            recognizerIntent.putExtra(
                EXTRA_LEANBACK_BADGE_PRESENT,
                mBadgeDrawable != null
            )
            return recognizerIntent
        }

    fun retrieveResults(searchQuery: String) {
        if (DEBUG) Log.v(
            TAG,
            "retrieveResults $searchQuery"
        )
        if (mProvider!!.onQueryTextChange(searchQuery)) {
            mStatus = mStatus and QUERY_COMPLETE.inv()
        }
    }

    fun submitQuery(query: String?) {
        queryComplete()
        if (null != mProvider) {
            mProvider!!.onQueryTextSubmit(query)
        }
    }

    fun queryComplete() {
        if (DEBUG) Log.v(
            TAG,
            "queryComplete"
        )
        mStatus = mStatus or QUERY_COMPLETE
        focusOnResults()
    }

    fun updateSearchBarVisibility() {
        mSearchBar!!.visibility = if( (mResultAdapter == null
                    ) || (mResultAdapter!!.size() == 0) || rowsSupportFragment!!.isShowingTitle) View.VISIBLE else View.GONE
    }

    fun updateSearchBarNextFocusId() {
        if (mSearchBar == null || mResultAdapter == null) {
            return
        }
        val viewId =
            if (mResultAdapter!!.size() == 0 || rowsSupportFragment == null) 0 else rowsSupportFragment!!.id
        mSearchBar!!.nextFocusDownId = viewId
    }

    fun updateFocus() {
        if (mResultAdapter != null && mResultAdapter!!.size() > 0 && rowsSupportFragment != null && rowsSupportFragment!!.adapter === mResultAdapter) {
            focusOnResults()
        } else {
            mSearchBar!!.requestFocus()
        }
    }

    private fun focusOnResults() {
        if (rowsSupportFragment == null || mResultAdapter!!.size() == 0) {
            return
        }
        if (rowsSupportFragment!!.requireView().requestFocus()) {
            mStatus = mStatus and RESULTS_CHANGED.inv()
        }
    }

    private fun onSetSearchResultProvider() {
        mHandler.removeCallbacks(mSetSearchResultProvider)
        mHandler.post(mSetSearchResultProvider)
    }

    fun releaseAdapter() {
        if (mResultAdapter != null) {
            mResultAdapter!!.unregisterObserver(mAdapterObserver)
            mResultAdapter = null
        }
    }

    fun executePendingQuery() {
        if (null != mPendingQuery && null != mResultAdapter) {
            val query: String = mPendingQuery!!
            mPendingQuery = null
            retrieveResults(query)
        }
    }

    private fun applyExternalQuery() {
        if (mExternalQuery == null || mSearchBar == null) {
            return
        }
        mSearchBar!!.setSearchQuery(mExternalQuery!!.mQuery)
        if (mExternalQuery!!.mSubmit) {
            submitQuery(mExternalQuery!!.mQuery)
        }
        mExternalQuery = null
    }

    private fun readArguments(args: Bundle?) {
        if (null == args) {
            return
        }
        if (args.containsKey(ARG_QUERY)) {
            setSearchQuery(args.getString(ARG_QUERY))
        }
        if (args.containsKey(ARG_TITLE)) {
            title = args.getString(ARG_TITLE)
        }
    }

    private fun setSearchQuery(query: String?) {
        mSearchBar!!.setSearchQuery(query)
    }

    class ExternalQuery(var mQuery: String, var mSubmit: Boolean)

    companion object {
        val TAG = SearchSupportFragment::class.java.simpleName
        const val DEBUG = false
        private const val EXTRA_LEANBACK_BADGE_PRESENT = "LEANBACK_BADGE_PRESENT"
        private val ARG_PREFIX = SearchSupportFragment::class.java.canonicalName
        private val ARG_QUERY: String = ARG_PREFIX + ".query"
        private val ARG_TITLE: String = ARG_PREFIX + ".title"
        const val SPEECH_RECOGNITION_DELAY_MS: Long = 300
        const val RESULTS_CHANGED = 0x1
        const val QUERY_COMPLETE = 0x2
        const val AUDIO_PERMISSION_REQUEST_CODE = 0

        /**
         * @param args Bundle to use for the arguments, if null a new Bundle will be created.
         */
        @JvmOverloads
        fun createArgs(args: Bundle?, query: String?, title: String? = null): Bundle {
            var args = args
            if (args == null) {
                args = Bundle()
            }
            args.putString(ARG_QUERY, query)
            args.putString(ARG_TITLE, title)
            return args
        }

        /**
         * Creates a search fragment with a given search query.
         *
         *
         * You should only use this if you need to start the search fragment with a
         * pre-filled query.
         *
         * @param query The search query to begin with.
         * @return A new SearchSupportFragment.
         */
        fun newInstance(query: String?): SearchSupportFragment {
            val fragment = SearchSupportFragment()
            val args = createArgs(null, query)
            fragment.arguments = args
            return fragment
        }
    }
}