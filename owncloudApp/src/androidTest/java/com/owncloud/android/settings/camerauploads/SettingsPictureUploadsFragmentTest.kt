/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.settings.camerauploads

import android.content.Context
import android.os.Build
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.R
import com.owncloud.android.db.PreferenceManager
import com.owncloud.android.presentation.ui.settings.fragments.SettingsPictureUploadsFragment
import com.owncloud.android.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import com.owncloud.android.utils.matchers.verifyPreference
import com.owncloud.android.utils.mockIntent
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SettingsPictureUploadsFragmentTest {

    private lateinit var fragmentScenario: FragmentScenario<SettingsPictureUploadsFragment>

    private lateinit var prefEnablePictureUploads: SwitchPreferenceCompat
    private lateinit var prefPictureUploadsPath: Preference
    private lateinit var prefPictureUploadsOnWifi: CheckBoxPreference
    private lateinit var prefPictureUploadsSourcePath: Preference
    private lateinit var prefPictureUploadsBehaviour: ListPreference

    private lateinit var picturesViewModel: SettingsPictureUploadsViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        picturesViewModel = mockk(relaxUnitFun = true)

        every { picturesViewModel.getPictureUploadsPath() } returns "/Upload/Path/"
        every { picturesViewModel.getPictureUploadsSourcePath() } returns "/Upload/Source/Path/"
        every { picturesViewModel.isPictureUploadEnabled() } returns false

        stopKoin()

        startKoin {
            context
            modules(
                module(override = true) {
                    viewModel {
                        picturesViewModel
                    }
                }
            )
        }

        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_ownCloud)
        fragmentScenario.onFragment { fragment ->
            prefEnablePictureUploads = fragment.findPreference(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED)!!
            prefPictureUploadsPath = fragment.findPreference(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH)!!
            prefPictureUploadsOnWifi = fragment.findPreference(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY)!!
            prefPictureUploadsSourcePath = fragment.findPreference(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE)!!
            prefPictureUploadsBehaviour = fragment.findPreference(PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR)!!
        }

        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun pictureUploadsView() {
        prefEnablePictureUploads.verifyPreference(
            keyPref = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_ENABLED,
            titlePref = context.getString(R.string.prefs_camera_picture_upload),
            summaryPref = context.getString(R.string.prefs_camera_picture_upload_summary),
            visible = true,
            enabled = true
        )
        assertFalse(prefEnablePictureUploads.isChecked)

        prefPictureUploadsPath.verifyPreference(
            keyPref = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_PATH,
            titlePref = context.getString(R.string.prefs_camera_picture_upload_path_title),
            summaryPref = "/Upload/Path",
            visible = true,
            enabled = false
        )

        prefPictureUploadsOnWifi.verifyPreference(
            keyPref = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_WIFI_ONLY,
            titlePref = context.getString(R.string.prefs_camera_picture_upload_on_wifi),
            visible = true,
            enabled = false
        )
        assertFalse(prefPictureUploadsOnWifi.isChecked)

        val comment =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) context.getString(
                R.string.prefs_camera_upload_source_path_title_optional
            )
            else context.getString(
                R.string.prefs_camera_upload_source_path_title_required
            )
        prefPictureUploadsSourcePath.verifyPreference(
            keyPref = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_SOURCE,
            titlePref = String.format(prefPictureUploadsSourcePath.title.toString(), comment),
            summaryPref = "/Upload/Source/Path",
            visible = true,
            enabled = false
        )

        prefPictureUploadsBehaviour.verifyPreference(
            keyPref = PreferenceManager.PREF__CAMERA_PICTURE_UPLOADS_BEHAVIOUR,
            titlePref = context.getString(R.string.prefs_camera_upload_behaviour_title),
            summaryPref = context.getString(R.string.pref_behaviour_entries_keep_file),
            visible = true,
            enabled = false
        )
    }

    @Test
    fun enablePictureUploads() {
        firstEnablePictureUploads()
        assertTrue(prefEnablePictureUploads.isChecked)
        assertTrue(prefPictureUploadsPath.isEnabled)
        assertTrue(prefPictureUploadsOnWifi.isEnabled)
        assertTrue(prefPictureUploadsSourcePath.isEnabled)
        assertTrue(prefPictureUploadsBehaviour.isEnabled)
    }

    @Test
    fun disablePictureUploadsAccept() {
        firstEnablePictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_yes)).perform(click())
        assertFalse(prefEnablePictureUploads.isChecked)
        assertFalse(prefPictureUploadsPath.isEnabled)
        assertFalse(prefPictureUploadsOnWifi.isEnabled)
        assertFalse(prefPictureUploadsSourcePath.isEnabled)
        assertFalse(prefPictureUploadsBehaviour.isEnabled)
    }

    @Test
    fun disablePictureUploadsRefuse() {
        firstEnablePictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(R.string.common_no)).perform(click())
        assertTrue(prefEnablePictureUploads.isChecked)
        assertTrue(prefPictureUploadsPath.isEnabled)
        assertTrue(prefPictureUploadsOnWifi.isEnabled)
        assertTrue(prefPictureUploadsSourcePath.isEnabled)
        assertTrue(prefPictureUploadsBehaviour.isEnabled)
    }

    @Test
    fun openUploadPathPicker() {
        firstEnablePictureUploads()
        onView(withText(R.string.prefs_camera_picture_upload_path_title))
        // I think in this case we don't need action
        //mockIntent(
        //    action =
        //)
    }

    private fun firstEnablePictureUploads() {
        onView(withText(R.string.prefs_camera_picture_upload)).perform(click())
        onView(withText(android.R.string.ok)).perform(click())
    }
}
