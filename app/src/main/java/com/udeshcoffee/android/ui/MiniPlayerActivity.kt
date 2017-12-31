package com.udeshcoffee.android.ui

import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
import android.util.Log
import android.view.View
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.udeshcoffee.android.*
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.main.MainActivity
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerFragment
import com.udeshcoffee.android.ui.miniplayer.MiniPlayerPresenter
import com.udeshcoffee.android.ui.player.player.PlayerFragment
import com.udeshcoffee.android.ui.player.player.PlayerPresenter
import com.udeshcoffee.android.ui.player.queue.QueueFragment
import com.udeshcoffee.android.ui.player.queue.QueuePresenter
import com.udeshcoffee.android.utils.Injection
import com.udeshcoffee.android.views.FadableLayout
import io.reactivex.disposables.Disposable

/**
 * Created by Udathari on 8/25/2017.
 */

open class MiniPlayerActivity : BaseActivity() {

    open lateinit var miniPlayerPresenter: MiniPlayerPresenter

    private lateinit var playerFragment: PlayerFragment
    private lateinit var queueFragment: QueueFragment
    private lateinit var playerPresenter: PlayerPresenter
    private lateinit var queuePresenter: QueuePresenter

    private var isQueueVisible = false

    open lateinit var drawer: NavigationView
    open lateinit var drawerLayout: DrawerLayout
    private lateinit var controlBack: FadableLayout
    private lateinit var slidingPanel: SlidingUpPanelLayout

    // State
    private var controlBackSongId: Long = -1

    lateinit var broadcastDisposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawer = findViewById(R.id.navigation)
        drawerLayout = findViewById(R.id.drawer_layout)

        slidingPanel = findViewById(R.id.slidingpanel)
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
        slidingPanel.addPanelSlideListener(object : SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                if (slideOffset <= 0.2f) {
                    miniPlayerPresenter.setAlpha(1f)
                } else if (slideOffset > 0.2f && slideOffset <= 0.6f) {
                    miniPlayerPresenter.setVisibility(View.VISIBLE)
                    miniPlayerPresenter.setAlpha(1 - (slideOffset - 0.2f) / 0.4f)
                } else if (slideOffset > 0.6f) {
                    miniPlayerPresenter.setVisibility(View.INVISIBLE)
                }
            }

            override fun onPanelStateChanged(panel: View?, previousState: SlidingUpPanelLayout.PanelState?, newState: SlidingUpPanelLayout.PanelState?) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED)
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                else if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED || newState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    checkAndChangeNavigationLock()
                }

                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    miniPlayerPresenter.setVisibility(View.VISIBLE)
                    miniPlayerPresenter.setAlpha(1f)
                }
            }

        })

        // Control Back
        controlBack = findViewById(R.id.controls_back)
        // Mini Player Fragment
        val miniPlayerFragment = supportFragmentManager.findFragmentById(R.id.mini_player_container)
                as MiniPlayerFragment? ?: MiniPlayerFragment().also {
            addFragmentToActivity(it, R.id.mini_player_container)
        }

        miniPlayerPresenter = MiniPlayerPresenter(miniPlayerFragment)

        val fragment = supportFragmentManager.findFragmentById(R.id.player_container)
        when (fragment) {
            is PlayerFragment -> {
                playerFragment = fragment
                queueFragment = QueueFragment()
            }
            is QueueFragment -> {
                queueFragment = fragment
                playerFragment = PlayerFragment().also {
                    addFragmentToActivity(it, R.id.player_container)
                    ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
                }
            }
            null -> {
                playerFragment = PlayerFragment().also {
                    addFragmentToActivity(it, R.id.player_container)
                    ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
                }
                queueFragment = QueueFragment()
            }
        }

        playerPresenter = PlayerPresenter(playerFragment, sharedPreferences,
                Injection.provideDataRepository(this.applicationContext))
        queuePresenter = QueuePresenter(queueFragment)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)
        filter.addAction(MusicService.InternalIntents.SERVICE_CONNECTED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            setControlBack()
                        }
                        MusicService.InternalIntents.SERVICE_CONNECTED -> {
                            setControlBack()
                        }
                    }
                }
        setControlBack()
    }

    override fun onPause() {
        super.onPause()
        if (!broadcastDisposable.isDisposed)
            broadcastDisposable.dispose()
    }

    fun openNowPlay(){
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
    }

    fun closeNowPlay(){
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
    }

    fun hideNowPlay(){
        slidingPanel.panelState = SlidingUpPanelLayout.PanelState.HIDDEN
    }

    fun initNowPlay(){
        if (slidingPanel.panelState == SlidingUpPanelLayout.PanelState.HIDDEN)
            closeNowPlay()
    }

    private fun setControlBack(){
        val service = getService()
        service?.currentSong()?.let {
            if (controlBackSongId != it.id) {
                it.loadFadableBack(this, controlBack)
                controlBackSongId = it.id
            }
        }
    }

    fun showQueue() {
        isQueueVisible = true
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, queueFragment)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        transaction.commit()
        slidingPanel.isTouchEnabled = false
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    fun showPlayer() {
        isQueueVisible = false
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.player_container, playerFragment)
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        transaction.commit()
        slidingPanel.isTouchEnabled = true
        ViewCompat.requestApplyInsets(findViewById(R.id.player_container))
    }

    open fun canChangeMainContent(): Boolean {
        return slidingPanel.panelState != SlidingUpPanelLayout.PanelState.EXPANDED
    }

    open fun checkAndChangeNavigationLock() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            when (supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name){
                MainActivity.Fragments.PLAYLISTS -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) }
                MainActivity.Fragments.EQUALIZER -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) }
                MainActivity.Fragments.FAVORITES -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) }
                MainActivity.Fragments.HOME -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) }
                MainActivity.Fragments.LIBRARY -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) }
                else -> { drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) }
            }
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressedSuper")
        when {
            isQueueVisible -> showPlayer()
            slidingPanel.panelState == SlidingUpPanelLayout.PanelState.EXPANDED -> closeNowPlay()
            else -> {
                moveTaskToBack(true)
            }
        }
    }
}